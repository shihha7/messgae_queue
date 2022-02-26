package top.shen.ssqmq_client.config;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import top.shen.ssqmq_client.handle.ClientHandle;

public class ChannelInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel sc) throws Exception {

        ChannelPipeline pipeline = sc.pipeline();

        //maxFrameLength表示这一贞最大的大小
        //delimiter表示分隔符，我们需要先将分割符写入到ByteBuf中，然后当做参数传入；
        //pipeline.addLast(new DelimiterBasedFrameDecoder(2048, Delimiters.lineDelimiter()[0]));
        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
        //添加Netty空闲超时检查的支持
        //第一个参读空闲超时,第二个参写空闲超时, 第三个参读写空闲超时
        pipeline.addLast(new IdleStateHandler(5,5,10));
        //自定义处理器
        pipeline.addLast(new ClientHandle());

    }

}
