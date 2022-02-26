package top.shen.ssqmq_server.task;


import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.shen.ssqmq_server.core.Message;
import top.shen.ssqmq_server.handle.ServiceHandle;
import top.shen.ssqmq_server.store.MessageFileQueue;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AddOutTask implements Runnable{


    private static final Logger logger = LoggerFactory.getLogger(AddOutTask.class);

    @Autowired
    private MessageFileQueue messageFileQueue;

    @Autowired
    private OutMessageTask outMessageTask;

    private boolean start = false;

    @Override
    public void run() {

        try {
            Thread.sleep(20000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (this.start) {
            String s = "";
            try {
                ConcurrentHashMap<String, List<String>> listenChannelQueueMap = ServiceHandle.getListenChannelMap();
                for (Map.Entry<String,List<String>> map : listenChannelQueueMap.entrySet()){
                    s = messageFileQueue.pollMessage(map.getKey());
                    if (s != null && !s.trim().equals("")) {

                        Message message = JSONObject.parseObject(s, Message.class);

                        outMessageTask.addTask(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("AddOutTask error " + s);
                throw new RuntimeException(e);
            }
        }

    }

    @PostConstruct
    public void start() {
        this.start = true;
        new Thread(this).start();
    }

    public boolean getStart() {
        return start;
    }

}





