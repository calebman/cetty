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



* 实现RequestMapping注解，用于标识处理器或者控制器对应匹配的接口地址。

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String[] value() default {};
}
```



* 提供启动入口，程序启动时创建Spring容器，并基于Spring初始化必要组件

1. 提供程序入口类

```java
public class CettyBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(CettyBootstrap.class);
    private static final String DEFAULT_SPRING_XMLPATH = "classpath:applicantContext.xml";
    private static final String DEFAULT_HTTP_SERVER_BEAN_NAME = "defaultHttpServer";

    public static void create() {
        create(DEFAULT_SPRING_XMLPATH);
    }

    public static void create(String springXmlpath) {
        if (StringUtils.isEmpty(springXmlpath)) {
            springXmlpath = DEFAULT_SPRING_XMLPATH;
        }
        logger.debug("spring框架配置文件地址为{}", springXmlpath);
        try {
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(springXmlpath.split("[,\\s]+"));
            context.start();
            logger.debug("spring框架启动成功");
            try {
                context.getBean(DEFAULT_HTTP_SERVER_BEAN_NAME, DefaultHttpServer.class);
            } catch (NoSuchBeanDefinitionException ex) {
                logger.warn("未配置HttpServer，采用默认配置启动");
                context.getAutowireCapableBeanFactory().createBean(DefaultHttpServer.class);
            }
        } catch (BeansException e) {
            e.printStackTrace();
        }
    }
}
```

2. 定义默认实现的HttpServer组件，随Spring容器启动时加载基于Netty的Web容器，并使用HandlerMapping组件初始化HttpPipelineInitializer管道，其中HandlerMapping如果未有用户定义则使用默认的DefaultHandlerMapping实现

```java
public class DefaultHttpServer extends ApplicationObjectSupport {
    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpServer.class);
    private static final String DEFAULT_HTTP_PORT = "8080";
    private static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";


    private String port;

    private HandlerMapping handlerMapping;

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public void initApplicationContext(ApplicationContext applicationContext) {
        beforeInit(applicationContext);
        initHandlerMapping(applicationContext);
        initServer();
    }

    void initHandlerMapping(ApplicationContext context) {
        try {
            this.handlerMapping = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
        } catch (NoSuchBeanDefinitionException ex) {
            this.handlerMapping = context.getAutowireCapableBeanFactory().createBean(DefaultHandlerMapping.class);
        }
    }

    void initServer() {
        logger.debug("初始化服务器");
        if (!HttpUtils.isPort(port)) {
            logger.warn("端口号不合法，使用默认端口{}", DEFAULT_HTTP_PORT);
            port = DEFAULT_HTTP_PORT;
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(Integer.parseInt(port)))
                    .childHandler(new HttpPipelineInitializer(handlerMapping));

            ChannelFuture f = b.bind().sync();
            logger.info("服务启动成功，监听{}端口", port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                workerGroup.shutdownGracefully().sync();
                bossGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void beforeInit(ApplicationContext applicationContext) {

    }

}
```

3. 提供默认的HandlerMapping实现类，负责匹配@RequestMapping注解下的处理函数

```java
public class DefaultHandlerMapping extends ApplicationObjectSupport implements HandlerMapping {
    Logger logger = LoggerFactory.getLogger(DefaultHandlerMapping.class);

    private static Map<String, HttpHandler> httpHandlerMap = new HashMap<String, HttpHandler>();

    @Override
    public void initApplicationContext(ApplicationContext context) throws BeansException {
        logger.debug("初始化处理匹配器");
        Map<String, Object> handles = context.getBeansWithAnnotation(Controller.class);
        try {
            for (Map.Entry<String, Object> entry : handles.entrySet()) {
                logger.debug("加载控制器{}", entry.getKey());
                loadHttpHandler(entry.getValue());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    void loadHttpHandler(Object value) throws IllegalAccessException, InstantiationException {
        Class clazz = value.getClass();
        Object clazzFromInstance = clazz.newInstance();
        Method[] method = clazz.getDeclaredMethods();
        for (Method m : method) {
            if (m.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = m.getAnnotation(RequestMapping.class);
                for (String url : requestMapping.value()) {
                    HttpHandler httpHandler = httpHandlerMap.get(url);
                    if (httpHandler == null) {
                        logger.info("加载url为{}的处理器{}", url, m.getName());
                        httpHandlerMap.put(url, new HttpHandler(clazzFromInstance, m));
                    } else {
                        logger.warn("url{}存在相同的处理器", url);
                    }
                }
            }
        }
    }

    @Override
    public HttpHandler getHadnler(FullHttpRequest request) {
        return httpHandlerMap.get(request.uri());
    }
}
```



* 当请求进入时通过HandlerMapping组件匹配处理器，如果匹配失败则返回404

```java
public class AllocHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private HandlerMapping handlerMapping;

    public AllocHandler(HandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    /*
    异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        HttpHandler httpHandler = handlerMapping.getHadnler(fullHttpRequest);
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

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus httpResponseStatus) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus, Unpooled.copiedBuffer(httpResponseStatus.toString(), CharsetUtil.UTF_8));
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
        CettyBootstrap.create();
    }
}
```

### 未来要做的

- [x] 与Spring框架集成，将核心组件托管给Spring容器统一管理
- [ ] 提供静态资源映射
- [ ] 修改映射策略将请求映射至一个流程（一个处理器多个拦截器）
- [ ] 支持使用模板语法进行视图解析