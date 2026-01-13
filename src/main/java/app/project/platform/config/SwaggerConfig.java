package app.project.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
            return new OpenAPI()
                .info(new Info()
                        .title("프로젝트 API 생성")
                        .description("프로젝트의 API 서버입니다.")
                        .version("1.0.0"));
    }

}
