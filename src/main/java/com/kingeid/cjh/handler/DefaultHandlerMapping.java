package com.kingeid.cjh.handler;

import com.kingeid.cjh.annotation.RequestMapping;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
