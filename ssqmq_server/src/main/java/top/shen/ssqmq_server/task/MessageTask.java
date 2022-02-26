package top.shen.ssqmq_server.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public abstract class MessageTask<T> implements Runnable{

    protected static final Logger logger = LoggerFactory.getLogger(MessageTask.class);

    protected LinkedBlockingQueue<T> list = new LinkedBlockingQueue<>();

    private boolean start = false;

    public void addTask (T t) {

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

    public T pollTask () {
        LinkedBlockingQueue<T> list =  this.list;
        if (list.isEmpty()) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return list.poll();
    }

    public abstract void execute(T t);

    @Override
    public void run() {
        while (this.start){
            T t = null;
            try {
                t = pollTask();
                execute(t);
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



