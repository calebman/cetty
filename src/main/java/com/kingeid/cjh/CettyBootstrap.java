package com.kingeid.cjh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

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
