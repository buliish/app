package com.gec.seafood_traceability_system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.gec.seafood_traceability_system.mapper")
public class SeafoodTraceabilitySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeafoodTraceabilitySystemApplication.class, args);
    }

}
