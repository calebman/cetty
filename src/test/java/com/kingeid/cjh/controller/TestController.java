package com.kingeid.cjh.controller;


import com.alibaba.fastjson.JSONObject;
import com.kingeid.cjh.annotation.Controller;
import com.kingeid.cjh.annotation.RequestMapping;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.List;
import java.util.Set;

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
        System.out.println();
        System.out.println();
        System.out.println("----------");
        HttpHeaders httpHeaders = fullHttpRequest.headers();
        Set<String> names = httpHeaders.names();
        for (String name : names) {
            System.out.println(name + " : " + httpHeaders.get(name));
        }
        ByteBuf byteBuf = fullHttpRequest.content();
        byte[] byteArray = new byte[byteBuf.capacity()];
        byteBuf.readBytes(byteArray);
        System.out.println(new String(byteArray));
        System.out.println("----------");
        System.out.println();
        System.out.println();
        JSONObject json = new JSONObject();
        json.put("errCode", "00");
        json.put("errMsg", "0000000(成功)");
        json.put("data", null);
        return json;
    }
}
