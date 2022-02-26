package top.shen.ssqmq_producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"top.shen.ssqmq_client","top.shen.ssqmq_producer"})
public class SsqmqProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsqmqProducerApplication.class, args);
    }

}
