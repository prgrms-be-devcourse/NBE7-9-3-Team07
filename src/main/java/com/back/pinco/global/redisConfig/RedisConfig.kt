package com.back.pinco.global.redisConfig

import com.back.pinco.domain.pin.dto.PinCacheDto
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.GeoOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(
    private val redisProperties: RedisProperties
) {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration().apply {
            hostName = redisProperties.host
            port = redisProperties.port
            redisProperties.password?.let {
                password = RedisPassword.of(it)
            }
            database = redisProperties.database
        }
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(redisConnectionFactory())
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        return template
    }

    @Bean
    fun stringRedisTemplate(redisConnectionFactory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(redisConnectionFactory)
    }

    @Bean
    fun geoOperations(redisTemplate: RedisTemplate<String, Any>): GeoOperations<String, Any> {
        return redisTemplate.opsForGeo()
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
