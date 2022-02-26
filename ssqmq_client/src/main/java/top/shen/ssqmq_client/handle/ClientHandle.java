package top.shen.ssqmq_client.handle;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.shen.ssqmq_client.config.ClientConfig;
import top.shen.ssqmq_client.core.*;
import top.shen.ssqmq_client.core.Queue;
import top.shen.ssqmq_client.utils.PropertiesUtil;
import top.shen.ssqmq_client.utils.RandomUtil;
import top.shen.ssqmq_client.utils.SpringContextUtil;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandle extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandle.class);

    private static final String delayKey = "ssqmq.server.link-delay";
    private static final long defaultDelay = 3000L;

    private static final ConcurrentHashMap<String, List<AbstractListener>> listenMap = new ConcurrentHashMap<>();

    private static AtomicInteger resetCount = new AtomicInteger();
    private static final int maxResetCount = 3;

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        Message message = JSONObject.parseObject(msg, Message.class);
        if (message == null) {
            return;
        }
        if (message.getType() == Constants.Type.CONNECT) {
            resConnect(message.getMessage());
            return;
        }
        List<AbstractListener> listeners = listenMap.get(message.getQueueName());

        listeners.get(RandomUtil.random(listeners.size())).listen(message.getMessage());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE){

                if (ctx.channel().isActive()) {
                    Message message = new Message(Constants.Type.HEART_BEAT,"","","HEART_BEAT");
                    ctx.channel().writeAndFlush(JSONObject.toJSONString(message));
                }

            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        String delay = PropertiesUtil.getProperties(delayKey);
        logger.info("client channelActive,{}",delay);
        if (delay == null){
            Thread.sleep(defaultDelay);
        } else {
            Thread.sleep(Long.parseLong(delay));
        }

        String listenName = listen();

        Map<String, Queue> queues = SpringContextUtil.getBeansOfType(Queue.class);
        StringBuffer name = new StringBuffer();

        Iterator<Map.Entry<String, Queue>> iterator = queues.entrySet().iterator();
        while (iterator.hasNext()){
            name.append(iterator.next().getValue().getName());
            if (iterator.hasNext()){
                name.append(",");
            }
        }

        JSONObject msg = new JSONObject();
        msg.put("userName",ClientConfig.getUserName());
        msg.put("password",ClientConfig.getPassword());

        Message message = new Message(Constants.Type.CONNECT,name.toString(),listenName,msg.toString());
        ctx.channel().writeAndFlush(JSONObject.toJSONString(message));

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        if (SpringContextUtil.getApplicationContext() != null && resetCount.get() < maxResetCount) {
            //重连
            logger.info("channelInactive doStart");
            ClientConfig config = SpringContextUtil.getBean(ClientConfig.class);
            config.start();
            resetCount.incrementAndGet();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info(" client exceptionCaught");
        cause.printStackTrace();
        ctx.close();
    }

    private String listen() throws NoSuchMethodException {
        StringBuffer listenName = new StringBuffer();
        Set<String> set = new HashSet<>();
        Map<String, AbstractListener> listens = SpringContextUtil.getBeansOfType(AbstractListener.class);

        Iterator<Map.Entry<String, AbstractListener>> iterator = listens.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, AbstractListener> next = iterator.next();


            AbstractListener value = next.getValue();
            Method listen = value.getClass().getDeclaredMethod("listen", String.class);
            SsqMqListener annotation = listen.getAnnotation(SsqMqListener.class);

            if (!listenMap.containsKey(annotation.queue())) {
                List<AbstractListener> listenerList = new ArrayList<>();
                listenerList.add(next.getValue());
                listenMap.put(annotation.queue(),listenerList);
            } else {
                List<AbstractListener> listeners = listenMap.get(annotation.queue());
                listeners.add(next.getValue());
            }

            set.add(annotation.queue());
        }

        Iterator<String> iteratorSet = set.iterator();
        while (iteratorSet.hasNext()) {
            String next = iteratorSet.next();
            listenName.append(next);
            if (iteratorSet.hasNext()){
                listenName.append(",");
            }
        }

        return listenName.toString();

    }

    private void resConnect(String message){
        if ("success".equals(message)) {
            resetCount.set(0);
            logger.info("服务端连接成功->端口为{}  用户为{}",ClientConfig.getPort(),ClientConfig.getUserName());
        } else {
            logger.info("服务端连接失败->端口为{}  用户为{} error->{}",ClientConfig.getPort(),ClientConfig.getUserName(),message);
        }
    }
}
