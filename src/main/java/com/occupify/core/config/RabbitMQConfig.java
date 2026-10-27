package com.occupify.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.event.ContractEscrowLockedEvent;
import com.occupify.core.event.ContractRejectedEvent;
import com.occupify.core.event.UserActivatedEvent;
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

  @Value("${app.rabbitmq.project-exchange:occupify.project.exchange}")
  private String projectExchangeName;

  @Value("${app.rabbitmq.user-exchange:occupify.user.exchange}")
  private String userExchangeName;

  @Value("${app.rabbitmq.user-queue:occupify.core.user.queue}")
  private String userQueueName;

  @Value("${app.rabbitmq.user-activated-routing-key:user.activated}")
  private String userActivatedRoutingKey;

  @Value("${app.rabbitmq.dlx-exchange:occupify.core.dlx}")
  private String dlxExchangeName;

  @Value("${app.rabbitmq.dlq-queue:occupify.core.user.dlq}")
  private String dlqQueueName;

  @Value("${app.rabbitmq.dlq-routing-key:user.dlq}")
  private String dlqRoutingKey;

  @Value("${app.rabbitmq.contract-exchange:occupify.contract.exchange}")
  private String contractExchangeName;

  @Value("${app.rabbitmq.contract-core-queue:occupify.core.contract.queue}")
  private String contractCoreQueueName;

  @Value("${app.rabbitmq.contract-compensation-routing-key:contract.compensation.*}")
  private String contractCompensationRoutingKey;

  @Value("${app.rabbitmq.contract-escrow-locked-routing-key:contract.escrow.locked}")
  private String contractEscrowLockedRoutingKey;

  @Value("${app.rabbitmq.agent-rpc-exchange:occupify.agent.rpc}")
  private String agentRpcExchangeName;

  @Value("${app.rabbitmq.project-search-queue:occupify.core.rpc.project.search}")
  private String projectSearchQueueName;

  @Value("${app.rabbitmq.project-search-routing-key:core.project.search}")
  private String projectSearchRoutingKey;

  @Value("${app.rabbitmq.project-get-queue:occupify.core.rpc.project.get}")
  private String projectGetQueueName;

  @Value("${app.rabbitmq.project-get-routing-key:core.project.get}")
  private String projectGetRoutingKey;

  @Bean
  public DirectExchange agentRpcExchange() {
    return new DirectExchange(agentRpcExchangeName, true, false);
  }

  @Bean
  public Queue projectSearchQueue() {
    return QueueBuilder.durable(projectSearchQueueName).build();
  }

  @Bean
  public Binding projectSearchBinding(Queue projectSearchQueue, DirectExchange agentRpcExchange) {
    return BindingBuilder.bind(projectSearchQueue)
        .to(agentRpcExchange)
        .with(projectSearchRoutingKey);
  }

  @Bean
  public Queue projectGetQueue() {
    return QueueBuilder.durable(projectGetQueueName).build();
  }

  @Bean
  public Binding projectGetBinding(Queue projectGetQueue, DirectExchange agentRpcExchange) {
    return BindingBuilder.bind(projectGetQueue).to(agentRpcExchange).with(projectGetRoutingKey);
  }

  @Bean
  public TopicExchange notificationExchange() {
    return new TopicExchange(exchangeName, true, false);
  }

  @Bean
  public TopicExchange projectExchange() {
    return new TopicExchange(projectExchangeName, true, false);
  }

  @Bean
  public TopicExchange userExchange() {
    return new TopicExchange(userExchangeName, true, false);
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
  public Binding coreUserBinding(Queue coreUserQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(coreUserQueue).to(userExchange).with(userActivatedRoutingKey);
  }

  @Bean
  public TopicExchange contractExchange() {
    return new TopicExchange(contractExchangeName, true, false);
  }

  @Bean
  public Queue coreContractQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", dlxExchangeName);
    args.put("x-dead-letter-routing-key", dlqRoutingKey);
    return QueueBuilder.durable(contractCoreQueueName).withArguments(args).build();
  }

  @Bean
  public Binding coreContractCompensationBinding(
      Queue coreContractQueue, TopicExchange contractExchange) {
    return BindingBuilder.bind(coreContractQueue)
        .to(contractExchange)
        .with(contractCompensationRoutingKey);
  }

  @Bean
  public Binding coreContractEscrowLockedBinding(
      Queue coreContractQueue, TopicExchange contractExchange) {
    return BindingBuilder.bind(coreContractQueue)
        .to(contractExchange)
        .with(contractEscrowLockedRoutingKey);
  }

  @Bean
  public MessageConverter jackson2JsonMessageConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

    DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
    typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    typeMapper.setTrustedPackages("*");

    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put("com.occupify.identity.event.UserActivatedEvent", UserActivatedEvent.class);
    idClassMapping.put("com.occupify.core.event.UserActivatedEvent", UserActivatedEvent.class);
    idClassMapping.put("UserActivatedEvent", UserActivatedEvent.class);
    idClassMapping.put(
        "com.occupify.identity.event.UserRegisteredEvent", UserRegisteredEvent.class);
    idClassMapping.put("com.occupify.core.event.UserRegisteredEvent", UserRegisteredEvent.class);
    idClassMapping.put("UserRegisteredEvent", UserRegisteredEvent.class);
    idClassMapping.put(
        "com.occupify.core.event.ContractAcceptedEvent", ContractAcceptedEvent.class);
    idClassMapping.put("ContractAcceptedEvent", ContractAcceptedEvent.class);
    idClassMapping.put(
        "com.occupify.core.event.ContractRejectedEvent", ContractRejectedEvent.class);
    idClassMapping.put("ContractRejectedEvent", ContractRejectedEvent.class);
    idClassMapping.put(
        "com.occupify.core.event.ContractCompensationRequestedEvent",
        ContractCompensationRequestedEvent.class);
    idClassMapping.put(
        "ContractCompensationRequestedEvent", ContractCompensationRequestedEvent.class);
    idClassMapping.put(
        "com.occupify.payment.event.ContractEscrowLockedEvent", ContractEscrowLockedEvent.class);
    idClassMapping.put(
        "com.occupify.core.event.ContractEscrowLockedEvent", ContractEscrowLockedEvent.class);
    idClassMapping.put("ContractEscrowLockedEvent", ContractEscrowLockedEvent.class);
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
