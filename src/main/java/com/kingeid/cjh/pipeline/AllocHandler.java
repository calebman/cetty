package com.kingeid.cjh.pipeline;

import com.alibaba.fastjson.JSONObject;
import com.kingeid.cjh.handler.HandlerMapping;
import com.kingeid.cjh.handler.HttpHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.springframework.context.ApplicationContext;

/**
 * @author calebman
 * @date 2018/8/6
 * @description Http请求处理器
 */
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
