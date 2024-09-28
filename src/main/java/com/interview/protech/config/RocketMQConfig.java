package com.interview.protech.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Bean
    @Primary
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setProducerGroup(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setVipChannelEnabled(false);
        producer.setSendMsgTimeout(3000);
        rocketMQTemplate.setProducer(producer);
        return rocketMQTemplate;
    }
}