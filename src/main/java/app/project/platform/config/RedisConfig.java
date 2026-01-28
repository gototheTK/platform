package app.project.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * RedisTemplate: Redis와 간편하게 통신하기 위한 도구
     * - Key는 String으로, Value는 JSON으로 직렬화해서 저장하도록 설정합니다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화: String (우리가 아는 글자로 저장됨)
        template.setKeySerializer(new StringRedisSerializer());

        //  Value 직렬화: JSON (객체를 JSON 형태로 저장해서 눈으로 보기 편함)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

}
