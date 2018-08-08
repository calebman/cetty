package com.kingeid.cjh;

import com.kingeid.cjh.handler.DefaultHandlerMapping;
import com.kingeid.cjh.handler.HandlerMapping;
import com.kingeid.cjh.pipeline.HttpPipelineInitializer;
import com.kingeid.cjh.utils.HttpUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import java.net.InetSocketAddress;

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
