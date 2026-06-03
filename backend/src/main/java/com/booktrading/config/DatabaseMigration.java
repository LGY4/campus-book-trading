package com.booktrading.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigration implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 修复 method 列长度不足导致日志写入失败
        alterColumnIfShorter("t_operation_log", "method", "VARCHAR(200)");

        addColumnIfNotExists("t_comment", "parent_id", "BIGINT COMMENT '追评关联原评论ID'");
        addColumnIfNotExists("t_comment", "images_json", "TEXT COMMENT 'JSON数组，存储评价图片路径'");
        addColumnIfNotExists("t_order", "logistics_company", "VARCHAR(50)");
        addColumnIfNotExists("t_order", "tracking_number", "VARCHAR(100)");
        addColumnIfNotExists("t_order", "return_logistics_company", "VARCHAR(50)");
        addColumnIfNotExists("t_order", "return_tracking_number", "VARCHAR(100)");
        addColumnIfNotExists("t_order", "dispute_images", "TEXT COMMENT '纠纷证据图片JSON'");
    }

    private void addColumnIfNotExists(String table, String column, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("[Migration] Added column " + table + "." + column);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate column")) {
                // Column already exists, ignore
            } else {
                System.err.println("[Migration] Failed to add " + table + "." + column + ": " + e.getMessage());
            }
        }
    }

    private void alterColumnIfShorter(String table, String column, String newDefinition) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + newDefinition);
            System.out.println("[Migration] Altered " + table + "." + column + " to " + newDefinition);
        } catch (Exception e) {
            System.err.println("[Migration] Failed to alter " + table + "." + column + ": " + e.getMessage());
        }
    }
}
