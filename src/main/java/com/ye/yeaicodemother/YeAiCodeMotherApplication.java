package com.ye.yeaicodemother;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class YeAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(YeAiCodeMotherApplication.class, args);
    }

}
