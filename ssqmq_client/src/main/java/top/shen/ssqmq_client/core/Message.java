package top.shen.ssqmq_client.core;

public class Message {

    private int type;

    private String queueName;

    private String queueNameListen;

    private String message;

    public Message() {
    }

    public Message(int type, String queueName, String queueNameListen, String message) {
        this.type = type;
        this.queueName = queueName;
        this.queueNameListen = queueNameListen;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "  type=" + type +
                ", queueName='" + queueName + '\'' +
                ", queueNameListen='" + queueNameListen + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getQueueNameListen() {
        return queueNameListen;
    }

    public void setQueueNameListen(String queueNameListen) {
        this.queueNameListen = queueNameListen;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
