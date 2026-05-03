package com.compute.rental;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan(value = "com.compute.rental.modules", markerInterface = BaseMapper.class)
@ConfigurationPropertiesScan
@SpringBootApplication
public class ComputeRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComputeRentalApplication.class, args);
    }
}
