package com.occupify.core.listener;

import com.occupify.core.dto.base.ErrorResponse;
import com.occupify.core.exception.BaseException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component("agentRpcErrorHandler")
public class AgentRpcErrorHandler implements RabbitListenerErrorHandler {

  @Override
  public Object handleError(
      Message amqpMessage,
      Channel channel,
      org.springframework.messaging.Message<?> message,
      ListenerExecutionFailedException exception) {
    return handle(amqpMessage, exception);
  }

  @Override
  @SuppressWarnings("deprecation")
  public Object handleError(
      Message amqpMessage,
      org.springframework.messaging.Message<?> message,
      ListenerExecutionFailedException exception) {
    return handle(amqpMessage, exception);
  }

  private Object handle(Message amqpMessage, ListenerExecutionFailedException exception) {
    String traceId = null;
    if (amqpMessage != null && amqpMessage.getMessageProperties() != null) {
      Object header = amqpMessage.getMessageProperties().getHeader("x-trace-id");
      if (header != null) {
        traceId = header.toString();
      }
    }

    Throwable cause = findRootOrSignificantCause(exception);

    if (cause instanceof BaseException be) {
      log.warn(
          "[Agent RPC][traceId={}] Business error: status={}, code={}, message={}",
          traceId,
          be.getHttpStatus().value(),
          be.getCode(),
          be.getMessage());
      return new ErrorResponse(be.getHttpStatus().value(), be.getMessage(), be.getCode());
    }

    if (isInvalidArgumentException(cause)) {
      if (isInvalidPriceRange(cause)) {
        log.warn("[Agent RPC][traceId={}] Invalid price range error", traceId);
        return new ErrorResponse(400, "Invalid price range", "INVALID_PRICE_RANGE");
      }
      log.warn(
          "[Agent RPC][traceId={}] Invalid argument error: {}",
          traceId,
          cause.getClass().getSimpleName());
      return new ErrorResponse(400, "Invalid argument", "INVALID_ARGUMENT");
    }

    log.error("[Agent RPC][traceId={}] Unexpected error processing RPC request", traceId, cause);
    return new ErrorResponse(500, "Failed to process the request", "INTERNAL_SERVER_ERROR");
  }

  private boolean isInvalidPriceRange(Throwable t) {
    if (t == null) {
      return false;
    }
    String msg = t.getMessage();
    return msg != null && msg.contains("INVALID_PRICE_RANGE");
  }

  private boolean isInvalidArgumentException(Throwable t) {
    if (t == null) {
      return false;
    }
    if (t instanceof IllegalArgumentException) {
      return true;
    }

    String className = t.getClass().getName();
    return className.contains("ConversionException")
        || className.contains("JsonProcessingException")
        || className.contains("MethodArgument")
        || className.contains("InvalidFormatException")
        || className.contains("IllegalArgumentException");
  }

  private Throwable findRootOrSignificantCause(Throwable t) {
    Throwable current = t;
    while (current != null) {
      if (current instanceof BaseException) {
        return current;
      }
      if (isInvalidArgumentException(current)) {
        return current;
      }
      if (current.getCause() == null || current.getCause() == current) {
        break;
      }
      current = current.getCause();
    }
    return t.getCause() != null ? t.getCause() : t;
  }
}
