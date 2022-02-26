package top.shen.ssqmq_server.handle;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.shen.ssqmq_server.admin.Admin;
import top.shen.ssqmq_server.admin.mapper.AdminMapper;
import top.shen.ssqmq_server.core.Constants;
import top.shen.ssqmq_server.core.Message;
import top.shen.ssqmq_server.task.InMessageTask;
import top.shen.ssqmq_server.utils.RandomUtil;
import top.shen.ssqmq_server.utils.SpringContextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceHandle extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHandle.class);

    private static final ConcurrentHashMap<String, List<String>> productChannelMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<String>> listenChannelMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, String> productChannelQueueMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> listenChannelQueueMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();

    private static InMessageTask in;

    //一条消息不在一个bytebuf里 就存在historyMsg里
    private static volatile ConcurrentHashMap<String, String> historyProductMsgMap = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        synchronized (ServiceHandle.class) {
            if (msg == null || msg.trim().equals("")) {
                return;
            }

            //判断消息是不是 Constants.Type.SEND这种类型
            boolean messageTypeSend = msg.contains("\"type\":3");
            if (!messageTypeSend) {
                handleRead(ctx,msg);
                return;
            }
            String historyMsg = historyProductMsgMap.get(ctx.channel().id().asLongText());
            msg = historyMsg==null?"":historyMsg + msg;
            historyProductMsgMap.put(ctx.channel().id().asLongText(),"");
            //并发高的话 可能传多条消息  需要分解消息
            String[] msgArr = msg.split("\\}");

            for (int i = 0; i < msgArr.length; i++) {
                if (msgArr[i].trim().equals("")) {
                    continue;
                }
                if (i==msgArr.length-1) {
                    try {
                        if (!msgArr[i].contains("\"type\":") || !msgArr[i].contains("\"queueNameListen\":") ||
                                !msgArr[i].contains("\"queueName\":") || !msgArr[i].contains("\"message\":")) {
                            //消息没传完 给下个handle处理
                            //这种是防止一条不完全 但是部分字段是完整的
                            historyProductMsgMap.put(ctx.channel().id().asLongText(),msgArr[i]);
                            logger.warn("historyProductMsg end "+historyProductMsgMap.get(ctx.channel().id().asLongText()));
                            continue;
                        }
                        JSONObject.parseObject(msgArr[i]+"}", Message.class);
                    } catch (Exception e) {
                        //消息没传完 给下个handle处理
                        historyProductMsgMap.put(ctx.channel().id().asLongText(),msgArr[i]);
                        logger.warn("historyProductMsg end "+historyProductMsgMap.get(ctx.channel().id().asLongText()));
                        continue;
                    }
                }
                handleRead(ctx,msgArr[i]+"}");
            }
        }

    }

    private void handleRead (ChannelHandlerContext ctx, String msg) throws Exception{
        Message message = JSONObject.parseObject(msg, Message.class);
        if (message == null) {
            return;
        }
        int type = message.getType();

        //logger.info("----------"+message.toString());
        switch (type) {
            case Constants.Type.CONNECT:
                connect(ctx,message);
                break;
            case Constants.Type.HEART_BEAT:
                logger.info("心跳检查");
                break;
            case Constants.Type.SEND:
                addMessage(message);
                break;
            default:
                break;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {
            logger.info("ssq heart beat -> perfect");
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE){
                logger.error("长时间不活跃，关闭通道"+ctx.channel().remoteAddress());
                ctx.channel().id();
                ctx.channel().close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("出现异常，关闭通道"+ctx.channel().remoteAddress());
        close(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        logger.info("断开连接"+ctx.channel().remoteAddress());
        close(ctx);
    }

    private void connect(ChannelHandlerContext ctx, Message message) throws Exception {
        logger.info(message.toString());
        Message res = new Message(Constants.Type.CONNECT,"","");
        if (!checkUser(message)) {
            res.setMessage("username or password invalid");
            ctx.channel().writeAndFlush(JSONObject.toJSONString(res));
            ctx.channel().close();
            return;
        } else {
            res.setMessage("success");
            ctx.channel().writeAndFlush(JSONObject.toJSONString(res));
        }
        channelMap.put(ctx.channel().id().asLongText(),ctx.channel());

        connectP(ctx,message.getQueueName());

        connectL(ctx,message.getQueueNameListen());
    }

    private boolean checkUser(Message message) {
        if (message.getMessage() == null || message.getMessage().trim().equals("")) {
            return false;
        }
        JSONObject jsonObject = JSONObject.parseObject(message.getMessage());
        Object userName = jsonObject.get("userName");
        Object password = jsonObject.get("password");

        if (userName == null ||password == null) {
            return false;
        }
        AdminMapper adminMapper = SpringContextUtil.getBean(AdminMapper.class);

        Admin admin = adminMapper.getAdmin(userName.toString());
        if (admin == null || admin.getPassword() == null || !admin.getPassword().equals(password.toString())) {
            return false;
        }
        return true;
    }

    private void connectP(ChannelHandlerContext ctx,String names) throws Exception {
        productChannelQueueMap.put(ctx.channel().id().asLongText(),names);
        String[] nameArr = names.split("\\,");

        for (String n : nameArr) {
            List<String> channels = productChannelMap.get(n);
            if (channels == null) {
                channels = new ArrayList<>();
            }
            channels.add(ctx.channel().id().asLongText());
            productChannelMap.put(n,channels);
        }

    }
    private void connectL(ChannelHandlerContext ctx,String names) throws Exception {

        ctx.channel().id().asLongText();
        listenChannelQueueMap.put(ctx.channel().id().asLongText(),names);

        String[] nameArr = names.split("\\,");

        for (String n : nameArr) {
            List<String> channels = listenChannelMap.get(n);
            if (channels == null) {
                channels = new ArrayList<>();
            }
            channels.add(ctx.channel().id().asLongText());
            listenChannelMap.put(n,channels);
        }

    }

    private void close(ChannelHandlerContext ctx) throws Exception {

        closeP(ctx);

        closeL(ctx);

        channelMap.remove(ctx.channel().id().asLongText());

        ctx.channel().close();
    }

    private void closeP(ChannelHandlerContext ctx) throws Exception {
        String names = productChannelQueueMap.remove(ctx.channel().id().asLongText());
        if (names == null || names.trim().equals("")) {
            return;
        }
        String[] nameArr = names.split("\\,");

        for (String n : nameArr) {
            productChannelMap.get(n).remove(ctx.channel().id().asLongText());
            if (productChannelMap.get(n).size() == 0) {
                productChannelMap.remove(n);
            }
        }
    }

    private void closeL(ChannelHandlerContext ctx) throws Exception {
        String names = listenChannelQueueMap.remove(ctx.channel().id().asLongText());
        if (names == null || names.trim().equals("")) {
            return;
        }
        String[] nameArr = names.split("\\,");

        for (String n : nameArr) {
            listenChannelMap.get(n).remove(ctx.channel().id().asLongText());
            if (listenChannelMap.get(n).size() == 0) {
                listenChannelMap.remove(n);
            }
        }
    }

    private void addMessage(Message message) {
        if (in == null){
            in = SpringContextUtil.getBean(InMessageTask.class);
        }
        in.addTask(message);
    }

    public static void send (Message message) {
        boolean isListen = checkListen(message);
        if (!isListen) {
            return;
        }
        List<String> channelIDs = listenChannelMap.get(message.getQueueName());

        String channelID = "";
        for (int i = 0; i < channelIDs.size(); i++) {
            if (channelMap.get(channelIDs.get(i)).isWritable()) {
                channelID = channelIDs.get(i);
            }
        }
        //String channelID = channelIDs.get(RandomUtil.random(channelIDs.size()));
        channelMap.get(channelID).writeAndFlush(JSONObject.toJSONString(message));
    }

    public static boolean checkListen(Message message) {
        List<String> channelIDs = listenChannelMap.get(message.getQueueName());
        if (channelIDs == null || channelIDs.size() == 0) {
            return false;
        }
        boolean isWritable = false;
        for (int i = 0; i < channelIDs.size(); i++) {
            if (channelMap.get(channelIDs.get(i)).isWritable()) {
                isWritable = true;
            }
        }
        return isWritable;
    }

    public static ConcurrentHashMap<String, String> getListenChannelQueueMap() {
        return listenChannelQueueMap;
    }

    public static ConcurrentHashMap<String, List<String>> getListenChannelMap() {
        return listenChannelMap;
    }
}
