
product(生产者) 
    1->引入包
        <dependency>
            <groupId>top.shen</groupId>
            <artifactId>ssqmq_client</artifactId>
            <version>1.0.0</version>
        </dependency>
     2->yml文件
        ssqmq:
          server:
            port: 
            host: 
            username: 
            password: 
            link-delay: 
    3->创建队列
        @Bean
        public Queue qiang(){
            return new Queue("qiang");
        }
    4->注入SsqMqTemplate
        @Autowired
        private SsqMqTemplate template;
    5->生产消息
        template.send(queueName,message);
        
subscribe(消费订阅者)
    1->引包
        <dependency>
            <groupId>top.shen</groupId>
            <artifactId>ssqmq_client</artifactId>
            <version>1.0.0</version>
        </dependency>
    2->yml文件
         ssqmq:
           server:
             port: 
             host: 
             username: 
             password: 
             link-delay: 
    3->实现 AbstractListener
    4->引用注解 @SsqMqListener(queue = "qiang")
    @Component
    public class TestListen implements AbstractListener {
        
        @SsqMqListener(queue = "qiang")
        @Override
        public void listen(String message) {
            System.out.println("消费消息"+message);
        }
    }

服务端
    1下载ssqmq_server 然后部署一下
    2 yml文件
        ssqmq:
          server:
            port: 
            sobacklog: 
            datadir: 
            indexdir: 
 
         # messgae_queue
# messgae_queue
# messgae_queue
