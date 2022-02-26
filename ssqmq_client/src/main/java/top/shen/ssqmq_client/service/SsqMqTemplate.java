package top.shen.ssqmq_client.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import top.shen.ssqmq_client.core.Constants;
import top.shen.ssqmq_client.core.Message;
import top.shen.ssqmq_client.task.WriteMessageTask;

import javax.annotation.Resource;

@Component
public class SsqMqTemplate {

    @Resource
    private WriteMessageTask task;

    public void send(String queueName,String message){
        task.addTask(build(queueName,message));
    }

    private String build(String queueName,String message){
        Message msg = new Message(Constants.Type.SEND,queueName,"",message);
        return JSONObject.toJSONString(msg);
    }
}
