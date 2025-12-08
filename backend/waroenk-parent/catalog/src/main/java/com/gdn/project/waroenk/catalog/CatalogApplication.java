package com.gdn.project.waroenk.catalog;

import com.github.cloudyrock.spring.v5.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongock
@EnableAsync
public class CatalogApplication {

  public static void main(String[] args) {
    SpringApplication.run(CatalogApplication.class, args);
  }

}
