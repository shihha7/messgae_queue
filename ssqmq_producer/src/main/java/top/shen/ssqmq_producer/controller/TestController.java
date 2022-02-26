package top.shen.ssqmq_producer.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.shen.ssqmq_client.core.Queue;
import top.shen.ssqmq_client.service.SsqMqTemplate;

@RestController
public class TestController {

    @Bean
    public Queue qiang(){
        return new Queue("qiang");
    }

    @Autowired
    private SsqMqTemplate template;

    @GetMapping("/test")
    private void test () {

        for (int i = 0 ;i <10000;i++) {
            template.send("qiang","shenshengqiang"+i);
        }
    }
}
