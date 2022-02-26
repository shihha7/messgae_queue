package top.shen.ssqmq_subscribe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"top.shen.ssqmq_client","top.shen.ssqmq_subscribe"})
public class SsqmqSubscribeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsqmqSubscribeApplication.class, args);
    }

}
