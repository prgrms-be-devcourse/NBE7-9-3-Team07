package com.back.pinco.global.redis


import com.back.pinco.domain.pin.dto.PinCacheDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory()
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(redisConnectionFactory())

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        return template
    }

    //커스텀 탬플릿
    @Bean
    fun pinRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, PinCacheDto> {
        val template = RedisTemplate<String, PinCacheDto>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = GenericToStringSerializer(String::class.java)
        template.valueSerializer = Jackson2JsonRedisSerializer(PinCacheDto::class.java)
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun geoRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Long> {
        val template = RedisTemplate<String, Long>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericToStringSerializer(Long::class.java)
        template.afterPropertiesSet()
        return template
    }
}
