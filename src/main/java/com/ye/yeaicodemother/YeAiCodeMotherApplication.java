package com.ye.yeaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("com.ye.yeaicodemother.mapper")
public class YeAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(YeAiCodeMotherApplication.class, args);
    }

}
