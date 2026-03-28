package com.claudej;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.claudej")
@MapperScan("com.claudej.infrastructure.**.mapper")
public class ClaudeJApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaudeJApplication.class, args);
    }
}
