package top.shen.ssqmq_server.task;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.shen.ssqmq_server.core.Message;
import top.shen.ssqmq_server.store.MessageFileQueue;

@Component
public class InMessageTask extends MessageTask <Message>{

    @Autowired
    private MessageFileQueue messageFileQueue;

    @Override
    public void execute(Message message) {
        messageFileQueue.addMessage(message.getQueueName(), JSONObject.toJSONString(message));
    }
}
