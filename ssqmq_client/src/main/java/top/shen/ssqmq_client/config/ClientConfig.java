package top.shen.ssqmq_client.config;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import top.shen.ssqmq_client.utils.PropertiesUtil;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;

@Component
public class ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(ClientConfig.class);

    private static final String hostKey = "ssqmq.server.host";
    private static final String portKey = "ssqmq.server.port";

    private static final String userNameKey = "ssqmq.server.username";
    private static final String passwordKey = "ssqmq.server.password";

    private static String userName;
    private static String password;

    private static SocketChannel channel;

    private Bootstrap bootstrap;

    private EventLoopGroup group;

    private static String host;

    private static String port;

    public ClientConfig() {
        host = PropertiesUtil.getProperties(hostKey);
        port = PropertiesUtil.getProperties(portKey);

        userName = PropertiesUtil.getProperties(userNameKey);
        password = PropertiesUtil.getProperties(passwordKey);
        if (host == null){
            log.error("server host is null");
            throw new RuntimeException("server host is null");
        }
        if (port == null){
            log.error("server port is null");
            throw new RuntimeException("server port is null");
        }

        if (userName == null){
            log.error("server userName is null");
            throw new RuntimeException("server userName is null");
        }

        if (password == null){
            log.error("server password is null");
            throw new RuntimeException("server password is null");
        }

        group = new NioEventLoopGroup();


        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG,16384)
                .option(ChannelOption.TCP_NODELAY,true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.SO_SNDBUF, 1024*8)
                .option(ChannelOption.SO_RCVBUF, 1024*8)
                .remoteAddress(new InetSocketAddress(host,Integer.parseInt(port)))
                .handler(new ChannelInit());

    }

    @PostConstruct
    public void start() throws InterruptedException {

        //连接到服务端 connect是异步连接
        ChannelFuture future = bootstrap.connect().sync();
        channel = (SocketChannel)future.channel();
    }

    public static SocketChannel ssqMqChannel() {
        return channel;
    }

    public static String getUserName() {
        return userName;
    }

    public static String getPassword() {
        return password;
    }

    public static String getHost() {
        return host;
    }

    public static String getPort() {
        return port;
    }
}
