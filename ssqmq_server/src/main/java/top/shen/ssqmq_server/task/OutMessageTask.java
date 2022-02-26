package top.shen.ssqmq_server.task;

import org.springframework.stereotype.Component;
import top.shen.ssqmq_server.core.Message;
import top.shen.ssqmq_server.handle.ServiceHandle;

@Component
public class OutMessageTask extends MessageTask<Message>{


    @Override
    public Message pollTask() {
        Message peek = this.list.peek();
        if (peek == null || !ServiceHandle.checkListen(peek)) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.error("pollTask error");
                }
            }
        }
        return this.list.poll();
    }

    @Override
    public void execute(Message message) {
        try {
            Thread.sleep(10);
            ServiceHandle.send(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
