package com.brex.demo;

import com.brex.demo.config.SqliteDataDirInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class)
                .initializers(new SqliteDataDirInitializer())
                .run(args);
    }
}
