package com.example.chatapp.config;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "chat_room";
    public static final String EXCHANGE_NAME = "chat_room_exchange";

    @Bean
    public Queue chatRoomQueue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Exchange chatRoomExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding chatRoomBinding(Queue queue, Exchange chatRoomExchange) {
        return BindingBuilder.bind(queue).to(chatRoomExchange).with("app.#").noargs();
    }

}
