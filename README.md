## 使用Netty构建一个带注解的Http服务器

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

* 使用Netty自带的编解码、聚合器构建一个带有Http编解码功能的服务器
* 实现Controller以及RequestMapping两个核心注解
* 启动时扫描控制器加载用户编写的对应地址的接口业务处理器
* 将业务处理器的控制中心作为一个Channel加入Pipeline容器

