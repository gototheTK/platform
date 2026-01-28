package app.project.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.logging.Logger;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PlatformApplicationTests {

    Logger logger = Logger.getLogger(PlatformApplicationTests.class.toString());

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void redisConnectionTest() {
        //  1. 데이터 저장 (Key: "test", Value: "hello redis")
        redisTemplate.opsForValue().set("test", "hello redis");

        //  2. 데이터 조회
        String value = (String) redisTemplate.opsForValue().get("test");

        //  3. 검증
        logger.info("가져온 값: " + value);
        assertThat(value).isEqualTo("hello redis");

    }
	@Test
	void contextLoads() {
	}

}
