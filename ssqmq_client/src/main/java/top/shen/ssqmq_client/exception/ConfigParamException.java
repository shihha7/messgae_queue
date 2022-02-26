package top.shen.ssqmq_client.exception;

public class ConfigParamException extends RuntimeException {

    private String message;

    public ConfigParamException(String message) {
        this.message = message;
    }
}
