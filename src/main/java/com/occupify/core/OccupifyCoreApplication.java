package com.occupify.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OccupifyCoreApplication {

  public static void main(String[] args) {
    SpringApplication.run(OccupifyCoreApplication.class, args);
  }
}
