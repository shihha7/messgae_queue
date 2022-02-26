package top.shen.ssqmq_server.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.shen.ssqmq_server.utils.PropertiesUtil;

import javax.annotation.PostConstruct;

@Component
public class ServerConfig {


    private static final String portKey = "ssqmq.server.port";

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

    /**
     * tcp so_backlog
     */
    private static final String sobacklogKey = "ssqmq.server.sobacklog";

    /**
     * 主线程
     */
    private EventLoopGroup boss;

    /**
     * 工作线程
     */
    private EventLoopGroup work;

    /**
     * 启动类
     */
    private ServerBootstrap bootstrap;

    /**
     * 回调
     */
    private ChannelFuture future;

    public ServerConfig () {

        String port = PropertiesUtil.getProperties(portKey);
        if (port == null){
            throw new RuntimeException("server port is null");
        }

        String sobacklog = PropertiesUtil.getProperties(sobacklogKey);
        if (sobacklog == null){
            sobacklog = "2048";
        }

        //服务端线程组
        boss = new NioEventLoopGroup(2);
        work = new NioEventLoopGroup();

        bootstrap = new ServerBootstrap();
        bootstrap.group(boss,work)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.SO_BACKLOG,Integer.parseInt(sobacklog))
                .option(ChannelOption.TCP_NODELAY,true)
                .option(ChannelOption.SO_SNDBUF, 1024*8)
                .option(ChannelOption.SO_RCVBUF, 1024*8)
                .childHandler(new ChannelInit());

    }

    @PostConstruct
    public void start () {
        String port = PropertiesUtil.getProperties(portKey);
        if (port == null){
            throw new RuntimeException("server port is null");
        }
        //绑定端口 同步等待绑定成功
        future = bootstrap.bind(Integer.parseInt(port));

        logger.info("服务端启动成功->端口为{}",port);
    }
}
