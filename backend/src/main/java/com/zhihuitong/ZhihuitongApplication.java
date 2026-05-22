package com.zhihuitong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zhihuitong.mapper")
@SpringBootApplication
public class ZhihuitongApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhihuitongApplication.class, args);
    }
}

