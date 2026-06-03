package com.booktrading;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@MapperScan("com.booktrading.mapper")
@EnableScheduling
public class BookTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTradingApplication.class, args);
    }

    @EventListener(WebServerInitializedEvent.class)
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        System.out.println("========================================");
        System.out.println("  BookTrading started on port: " + port);
        System.out.println("  http://localhost:" + port);
        System.out.println("========================================");

        // Write port to file for frontend proxy discovery
        try {
            java.io.File portFile = new java.io.File("target/app.port");
            portFile.getParentFile().mkdirs();
            java.nio.file.Files.write(portFile.toPath(), String.valueOf(port).getBytes());
        } catch (Exception e) {
            System.err.println("Failed to write port file: " + e.getMessage());
        }
    }
}
