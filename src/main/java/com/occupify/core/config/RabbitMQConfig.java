package com.occupify.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.occupify.core.event.UserRegisteredEvent;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  @Value("${app.rabbitmq.exchange:occupify.notification.exchange}")
  private String exchangeName;

  @Value("${app.rabbitmq.user-queue:occupify.core.user.queue}")
  private String userQueueName;

  @Value("${app.rabbitmq.user-registered-routing-key:user.registered}")
  private String userRegisteredRoutingKey;

  @Value("${app.rabbitmq.dlx-exchange:occupify.core.dlx}")
  private String dlxExchangeName;

  @Value("${app.rabbitmq.dlq-queue:occupify.core.user.dlq}")
  private String dlqQueueName;

  @Value("${app.rabbitmq.dlq-routing-key:user.dlq}")
  private String dlqRoutingKey;

  @Bean
  public TopicExchange notificationExchange() {
    return new TopicExchange(exchangeName, true, false);
  }

  @Bean
  public DirectExchange coreDlxExchange() {
    return new DirectExchange(dlxExchangeName, true, false);
  }

  @Bean
  public Queue coreUserDlq() {
    return QueueBuilder.durable(dlqQueueName).build();
  }

  @Bean
  public Binding coreUserDlqBinding(Queue coreUserDlq, DirectExchange coreDlxExchange) {
    return BindingBuilder.bind(coreUserDlq).to(coreDlxExchange).with(dlqRoutingKey);
  }

  @Bean
  public Queue coreUserQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", dlxExchangeName);
    args.put("x-dead-letter-routing-key", dlqRoutingKey);
    return QueueBuilder.durable(userQueueName).withArguments(args).build();
  }

  @Bean
  public Binding coreUserBinding(Queue coreUserQueue, TopicExchange notificationExchange) {
    return BindingBuilder.bind(coreUserQueue)
        .to(notificationExchange)
        .with(userRegisteredRoutingKey);
  }

  @Bean
  public MessageConverter jackson2JsonMessageConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

    DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
    typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    typeMapper.setTrustedPackages("*");

    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put(
        "com.occupify.identity.event.UserRegisteredEvent", UserRegisteredEvent.class);
    idClassMapping.put("com.occupify.core.event.UserRegisteredEvent", UserRegisteredEvent.class);
    idClassMapping.put("UserRegisteredEvent", UserRegisteredEvent.class);
    typeMapper.setIdClassMapping(idClassMapping);

    converter.setJavaTypeMapper(typeMapper);
    return converter;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    return rabbitTemplate;
  }
}
