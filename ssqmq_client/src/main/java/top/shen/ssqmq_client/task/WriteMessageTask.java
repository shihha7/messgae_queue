package top.shen.ssqmq_client.task;


import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.shen.ssqmq_client.config.ClientConfig;

import java.util.concurrent.LinkedBlockingQueue;

@Component
public class WriteMessageTask implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(WriteMessageTask.class);

    protected LinkedBlockingQueue<String> list = new LinkedBlockingQueue<>();

    private boolean start = false;


    public void addTask (String t) {

        try {
            synchronized (this) {
                this.list.add(t);
                if (!this.start) {
                    this.start = true;
                    new Thread(this).start();
                }
                this.notify();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            logger.error("add task error" + t);
        }

    }

    public void pollTask (SocketChannel ssqMqChannel) {

        LinkedBlockingQueue<String> list =  this.list;
        if (list.isEmpty()) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        ssqMqChannel.writeAndFlush(list.poll());
    }

    @Override
    public void run() {
        while (this.start){
            SocketChannel ssqMqChannel = ClientConfig.ssqMqChannel();
            String t = null;
            try {
                if(ssqMqChannel != null && ssqMqChannel.isActive() && ssqMqChannel.isWritable()) {
                    pollTask(ssqMqChannel);
                } else {
                    logger.error("execute task error  {}  {} " , ssqMqChannel.isActive(),ssqMqChannel.isWritable() );
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("execute task error" + t );
            }
        }
    }

    public boolean getStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }
}
