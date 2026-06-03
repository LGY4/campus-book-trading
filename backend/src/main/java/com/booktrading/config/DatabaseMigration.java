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
        // 确保连接使用 utf8mb4
        jdbcTemplate.execute("SET NAMES utf8mb4");
        jdbcTemplate.execute("SET CHARACTER_SET_RESULTS = utf8mb4");

        // Schema migrations
        alterColumnIfShorter("t_operation_log", "method", "VARCHAR(200)");
        addColumnIfNotExists("t_comment", "parent_id", "BIGINT COMMENT '追评关联原评论ID'");
        addColumnIfNotExists("t_comment", "images_json", "TEXT COMMENT 'JSON数组，存储评价图片路径'");
        addColumnIfNotExists("t_order", "logistics_company", "VARCHAR(50)");
        addColumnIfNotExists("t_order", "tracking_number", "VARCHAR(100)");
        addColumnIfNotExists("t_order", "return_logistics_company", "VARCHAR(50)");
        addColumnIfNotExists("t_order", "return_tracking_number", "VARCHAR(100)");
        addColumnIfNotExists("t_order", "dispute_images", "TEXT COMMENT '纠纷证据图片JSON'");

        // 初始数据（init.sql 只建表，数据由这里插入，避免 Docker 文件编码问题）
        ensureAdminExists();
        ensureCategoriesExist();
    }

    private void ensureAdminExists() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE username = 'admin'", Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.update(
                    "INSERT INTO t_user (username, password, phone, nickname, role) VALUES (?, ?, ?, ?, ?)",
                    "admin",
                    "$2a$10$Zh0TR31oPWS2oEdx9mEfN.n2.4dq6sjbfEW0TTntRYGf0ueH7Wi62",
                    "13800000000",
                    "admin",
                    "ADMIN"
                );
                System.out.println("[Migration] 已创建管理员账号 (admin/admin123)");
            }
        } catch (Exception e) {
            System.err.println("[Migration] 检查管理员账号失败: " + e.getMessage());
        }
    }

    private void ensureCategoriesExist() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_category", Integer.class);
            if (count == null || count == 0) {
                // 表空，插入默认分类
                insertDefaultCategories();
            } else {
                // 检查是否有乱码数据
                String name = jdbcTemplate.queryForObject("SELECT name FROM t_category WHERE id = (SELECT MIN(id) FROM t_category)", String.class);
                if (name != null && isGarbled(name)) {
                    System.out.println("[Migration] 检测到分类数据乱码，正在修复...");
                    jdbcTemplate.execute("DELETE FROM t_category");
                    insertDefaultCategories();
                    System.out.println("[Migration] 分类数据已修复");
                }
            }
        } catch (Exception e) {
            System.err.println("[Migration] 检查分类数据失败: " + e.getMessage());
        }
    }

    private boolean isGarbled(String text) {
        // UTF-8 中文被错误解码后会出现大量 Latin-1 补充字符 (0xC0-0xFF)
        int suspicious = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0xC0 && c <= 0xFF) {
                suspicious++;
            }
        }
        // 如果超过 30% 的字符是可疑的 Latin-1 补充字符，认为是乱码
        return suspicious > 0 && suspicious * 3 >= text.length();
    }

    private void insertDefaultCategories() {
        String[] names = {"计算机科学", "数学", "英语外语", "文学小说", "经济管理", "理工教材", "社会科学", "艺术设计", "考试考证", "其他"};
        for (int i = 0; i < names.length; i++) {
            try {
                jdbcTemplate.update("INSERT INTO t_category (name, sort_order, status) VALUES (?, ?, 1)", names[i], i + 1);
            } catch (Exception e) {
                // ignore
            }
        }
        System.out.println("[Migration] 已插入默认分类数据");
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
