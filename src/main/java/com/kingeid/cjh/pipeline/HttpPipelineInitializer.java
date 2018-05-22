package com.kingeid.cjh.pipeline;


import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpPipelineInitializer extends ChannelInitializer<Channel> {
    //编解码处理器名称
    public final static String CODEC = "codec";
    //HTTP消息聚合处理器名称
    public final static String AGGEGATOR = "aggegator";
    //HTTP消息压缩处理器名称
    public final static String COMPRESSOR = "compressor";

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(CODEC, new HttpServerCodec());
        pipeline.addLast(AGGEGATOR, new HttpObjectAggregator(512 * 1024));
        pipeline.addLast(COMPRESSOR,new HttpContentCompressor());
        pipeline.addLast(new AllocHandler());
    }
}
