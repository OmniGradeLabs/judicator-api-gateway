package com.judicator.gateway.infrastructure.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for the Exam & Grading domain.
 *
 * <p>Topology:
 *
 * <ul>
 *   <li>Exchange: {@code exam.exchange} (TopicExchange, durable)
 *   <li>Queue: {@code grading_jobs} (durable — survives broker restart)
 *   <li>Routing key: {@code exam.grading.routing.key}
 * </ul>
 *
 * <p>All messages are serialised as JSON automatically via {@link Jackson2JsonMessageConverter}.
 */
@Configuration
public class RabbitMQConfig {

  public static final String GRADING_QUEUE = "grading_jobs";
  public static final String EXAM_EXCHANGE = "exam.exchange";
  public static final String GRADING_ROUTING_KEY = "exam.grading.routing.key";

  // ── Topology ────────────────────────────────────────────────────────────────

  @Bean
  public Queue gradingJobsQueue() {
    // durable = true: queue survives RabbitMQ broker restart
    return new Queue(GRADING_QUEUE, true);
  }

  @Bean
  public TopicExchange examExchange() {
    return new TopicExchange(EXAM_EXCHANGE, true, false);
  }

  @Bean
  public Binding gradingBinding(Queue gradingJobsQueue, TopicExchange examExchange) {
    return BindingBuilder.bind(gradingJobsQueue).to(examExchange).with(GRADING_ROUTING_KEY);
  }

  // ── Serialisation ────────────────────────────────────────────────────────────

  /**
   * Configures Jackson as the message converter so Java objects are automatically serialised to
   * JSON payloads. Both {@link RabbitTemplate} (publisher) and {@code @RabbitListener} (consumer)
   * pick this up automatically when it is declared as a {@code @Bean}.
   */
  @Bean
  public MessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * Overrides the default {@link RabbitTemplate} to inject the JSON converter so all {@code
   * convertAndSend} calls produce JSON messages without any extra configuration at the call-site.
   */
  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, MessageConverter jackson2JsonMessageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jackson2JsonMessageConverter);
    return template;
  }
}
