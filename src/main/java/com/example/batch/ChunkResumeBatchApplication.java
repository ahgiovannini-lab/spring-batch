package com.example.batch;

import com.example.batch.config.BatchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BatchProperties.class)
public class ChunkResumeBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChunkResumeBatchApplication.class, args);
    }
}
