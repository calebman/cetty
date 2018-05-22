package com.kingeid.cjh.handler;

import io.netty.handler.codec.http.FullHttpRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
