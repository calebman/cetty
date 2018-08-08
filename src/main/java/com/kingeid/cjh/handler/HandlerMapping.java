package com.kingeid.cjh.handler;

import io.netty.handler.codec.http.FullHttpRequest;

public interface HandlerMapping {
    HttpHandler getHadnler(FullHttpRequest request);
}
