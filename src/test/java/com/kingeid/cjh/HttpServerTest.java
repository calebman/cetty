package com.kingeid.cjh;

public class HttpServerTest {
    public static void main(String[] args) throws Exception {
        HttpServer httpServer = new HttpServer(8080);
        httpServer.start();
    }
}
