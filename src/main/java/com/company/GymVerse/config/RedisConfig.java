package com.company.GymVerse.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

    @Configuration
    @RequiredArgsConstructor
    @Log4j2
    public class RedisConfig {

        /**
         * Redis server host.
         */
        @Value("${spring.redis.host}")
        private String redisHost;

        /**
         * Redis server port.
         */
        @Value("${spring.redis.port}")
        private int redisPort;

        /**
         * Manually configure the Redis connection factory so it doesn't
         * default to localhost.
         *  @return LettuceConnectionFactory for Redis connections
         */
        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            log.info(
                    "Creating RedisConnectionFactory with host={} and port={}",
                    redisHost, redisPort);
            return new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration(redisHost, redisPort));
        }

        //  Initialize after full context is ready (safe & no circular refs)
        /**
         * Verifies the Redis connection upon application context refresh.
         *
         * @param event the context refreshed event
         */
        @EventListener(ContextRefreshedEvent.class)
        public void verifyRedisConnection(final ContextRefreshedEvent event) {
            try {
                RedisConnectionFactory factory = event
                        .getApplicationContext().getBean(
                                RedisConnectionFactory.class);
                String pong = factory.getConnection().ping();
                log.info(" Redis connected successfully: {}", pong);
            } catch (Exception e) {
                log.error(
                        " Redis connection failed during initialization: {}",
                        e.getMessage(), e);
            }
        }

        /**
         * Configures a RedisTemplate with String serialization for keys and values.
         *
         * @param connectionFactory the Redis connection factory
         * @return a configured RedisTemplate instance
         */
        @Bean
        public RedisTemplate<String, String> redisTemplate(
                final RedisConnectionFactory connectionFactory) {

            RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

            redisTemplate.setConnectionFactory(connectionFactory);

            redisTemplate.setKeySerializer(new StringRedisSerializer());

            redisTemplate.setValueSerializer(new StringRedisSerializer());

            redisTemplate.setHashKeySerializer(new StringRedisSerializer());

            redisTemplate.setHashValueSerializer(new StringRedisSerializer());

            redisTemplate.afterPropertiesSet();

            return redisTemplate;
        }
}
