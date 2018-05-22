package com.kingeid.cjh.handler;

import com.kingeid.cjh.annotation.Controller;
import com.kingeid.cjh.annotation.RequestMapping;
import com.kingeid.cjh.utils.AnnotationUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author calebman
 * @Date 2018-4-28
 * 通过注解的方式加载所有控制器
 */
public class HttpController {
    private static Map<String, HttpHandler> httpHandlerMap = new HashMap<String, HttpHandler>();

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

    public static HttpHandler getHandler(String url) {
        return httpHandlerMap.get(url);
    }
}
