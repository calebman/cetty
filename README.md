## 使用Netty构建一个带注解的Http服务器框架

### 要实现怎样的效果

> 一个SpringBoot框架搭建起来的项目发布接口服务是这样的
>
> SpringBoot搭建教程[点击这里](https://www.jianshu.com/p/95946d6b0c7d)

```java
@Controller
@RequestMapping("/v1/product")
public class DocController {

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public WebResult search(@PathVariable("id") Integer id) {
        logger.debug("获取指定产品接收产品id=>%d", id);
        if (id == null || "".equals(id)) {
            logger.debug("产品id不能为空");
            return WebResult.error(ERRORDetail.RC_0101001);
        }
        return WebResult.success(products.get(id));
    }
}
```

> 我希望我使用Netty构建的Web服务器也能使用这样便捷的注解方式去发布我的接口服务

### 该怎么做

<p align="center">
  <img src="https://github.com/calebman/cetty/blob/master/images/flow.png">
</p>

* 使用Netty自带的编解码、聚合器构建一个带有Http编解码功能的服务器这一点其实非常简单，Netty提供了对应的Http协议的编解码以及聚合器，我们只需要在管道初始化的时候加载它们。

```java
public class HttpPipelineInitializer extends ChannelInitializer<Channel> {
    //编解码处理器名称
    public final static String CODEC = "codec";
    //HTTP消息聚合处理器名称
    public final static String AGGEGATOR = "aggegator";
    //HTTP消息压缩处理器名称
    public final static String COMPRESSOR = "compressor";

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(CODEC, new HttpServerCodec());
        pipeline.addLast(AGGEGATOR, new HttpObjectAggregator(512 * 1024));
        pipeline.addLast(COMPRESSOR,new HttpContentCompressor());
        pipeline.addLast(new AllocHandler());
    }
}
```



* 实现Controller以及RequestMapping两个核心注解，Controller注解用于标识那些类是控制器，RequestMapping注解用于标识处理器或者控制器对应匹配的接口地址。

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String[] value() default {};
}
```



* 启动时扫描控制器加载用户编写的对应地址的接口业务处理器，实现这个功能我们需要做一下几步操作

1. 定义一个包扫描工具类扫描class文件并提供一个处理接口

```java
    /**
     * 包扫描工具
     * @param iPackage 根级包名
     * @param iWhat 处理回调
     */
    public static void scanPackage(String iPackage, IWhat iWhat) {
        String path = iPackage.replace(".", "/");
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        try {
            if (url != null && url.toString().startsWith("file")) {
                String filePath = URLDecoder.decode(url.getFile(), "utf-8");
                File dir = new File(filePath);
                List<File> fileList = new ArrayList<File>();
                fetchFileList(dir, fileList);
                for (File f : fileList) {
                    String fileName = f.getAbsolutePath();
                    if (fileName.endsWith(".class")) {
                        String nosuffixFileName = fileName.substring(8 + fileName.lastIndexOf("classes"), fileName.indexOf(".class"));
                        String filePackage = nosuffixFileName.replaceAll("\\\\", ".");
                        Class<?> clazz = Class.forName(filePackage);
                        iWhat.execute(f, clazz);
                    } else {
                        iWhat.execute(f, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

2. 定义处理器实体类，用于存储处理地址以及处理器

```java
/**
 * @author calebman
 * @Date 2018-4-28
 * 处理器执行类
 */
public class HttpHandler {
    private Object clazzFromInstance;
    private Method method;

    public HttpHandler(Object clazzFromInstance, Method method) {
        this.clazzFromInstance = clazzFromInstance;
        this.method = method;
    }

    public Object execute(FullHttpRequest fullHttpRequest) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        return method.invoke(this.clazzFromInstance, (Object) fullHttpRequest);
    }
}
```

3. 扫描包并加入处理器列表

```java
    public static void loadHandlers(String packageName) {
        AnnotationUtil.scanPackage(packageName, new AnnotationUtil.IWhat() {
            @Override
            public void execute(File file, Class<?> clazz) throws Exception {
                if (clazz != null && clazz.isAnnotationPresent(Controller.class)) {
                    System.out.println("load contorller " + clazz.getSimpleName());
                    Object clazzFromInstance = clazz.newInstance();
                    Method[] method = clazz.getDeclaredMethods();
                    for (Method m : method) {
                        if (m.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping requestMapping = m.getAnnotation(RequestMapping.class);
                            for (String url : requestMapping.value()) {
                                HttpHandler httpHandler = httpHandlerMap.get(url);
                                if (httpHandler == null) {
                                    System.out.println("load url " + url + " handler " + m.getName());
                                    httpHandlerMap.put(url, new HttpHandler(clazzFromInstance, m));
                                } else {
                                    System.err.println("url " + url + " has same handler");
                                }
                            }
                        }
                    }
                }
            }
        });
    }
```



* 编写业务处理器的控制中心

```java
public class AllocHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        HttpHandler httpHandler = HttpController.getHandler(fullHttpRequest.uri());
        if (httpHandler != null) {
            Object obj = httpHandler.execute(fullHttpRequest);
            if (obj instanceof String) {
                sendMessage(ctx, obj.toString());
            } else {
                sendMessage(ctx, JSONObject.toJSONString(obj));
            }
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
        }
    }

    private void sendMessage(ChannelHandlerContext ctx, String msg) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
        response.headers().set("Content-Type", "text/plain");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
```

### 测试与使用

* 建立一个TestController

```java
@Controller
public class TestController {

    @RequestMapping("/test")
    public String testHandler(FullHttpRequest fullHttpRequest) {
        return "1234";
    }

    @RequestMapping("/zx")
    public String zx(FullHttpRequest fullHttpRequest) {
        return "zhuxiong";
    }

    @RequestMapping("/obj")
    public Object obj(FullHttpRequest fullHttpRequest) {
        System.out.println("\n\n----------");
        HttpHeaders httpHeaders = fullHttpRequest.headers();
        Set<String> names = httpHeaders.names();
        for (String name : names) {
            System.out.println(name + " : " + httpHeaders.get(name));
        }
        System.out.println("");
        ByteBuf byteBuf = fullHttpRequest.content();
        byte[] byteArray = new byte[byteBuf.capacity()];
        byteBuf.readBytes(byteArray);
        System.out.println(new String(byteArray));
        System.out.println("----------\n\n");

        JSONObject json = new JSONObject();
        json.put("errCode", "00");
        json.put("errMsg", "0000000(成功)");
        json.put("data", null);
        return json;
    }
}
```

* 启动服务

```java
public class HttpServerTest {
    public static void main(String[] args) throws Exception {
        HttpServer httpServer = new HttpServer(8080);
        httpServer.start();
    }
}
```

### 未来要做的

- [ ] 提供静态资源映射
- [ ] 修改映射策略将请求映射至一个流程（一个处理器多个拦截器）
- [ ] 支持使用模板语法进行视图解析