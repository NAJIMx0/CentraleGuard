package com.najim.plccommandservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PlcCommandServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlcCommandServiceApplication.class, args);
    }

}
