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

        // 确保连接使用 utf8mb4
        jdbcTemplate.execute("SET NAMES utf8mb4");

        // 修复分类乱码：如果分类数据存在乱码，删除后重新插入
        fixCategoryEncoding();

        // 确保管理员账号存在
        ensureAdminExists();
    }

    private void fixCategoryEncoding() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_category", Integer.class);
            if (count != null && count > 0) {
                // 检查是否有乱码数据（第一条记录的 name）
                String name = jdbcTemplate.queryForObject("SELECT name FROM t_category LIMIT 1", String.class);
                if (name != null && containsGarbledText(name)) {
                    System.out.println("[Migration] 检测到分类数据乱码，正在修复...");
                    jdbcTemplate.execute("DELETE FROM t_category");
                    insertDefaultCategories();
                    System.out.println("[Migration] 分类数据已修复");
                }
            } else {
                insertDefaultCategories();
            }
        } catch (Exception e) {
            System.err.println("[Migration] 修复分类数据失败: " + e.getMessage());
        }
    }

    private boolean containsGarbledText(String text) {
        // 检查是否包含典型的 UTF-8 乱码特征
        for (char c : text.toCharArray()) {
            if (c == '�' || (c >= '' && c <= '')) {
                return true;
            }
        }
        // 如果包含非 ASCII 但不在中文范围内，可能是乱码
        for (char c : text.toCharArray()) {
            if (c > 127 && !Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
                && !Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
                && !Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION)
                && !Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS)) {
                // 可能是乱码
                if (c >= 'À' && c <= 'ÿ') {
                    return true;
                }
            }
        }
        return false;
    }

    private void insertDefaultCategories() {
        String[] categories = {"计算机科学", "数学", "英语外语", "文学小说", "经济管理", "理工教材", "社会科学", "艺术设计", "考试考证", "其他"};
        for (int i = 0; i < categories.length; i++) {
            try {
                jdbcTemplate.update("INSERT INTO t_category (name, sort_order, status) VALUES (?, ?, 1)", categories[i], i + 1);
            } catch (Exception e) {
                // ignore duplicate
            }
        }
        System.out.println("[Migration] 已插入默认分类数据");
    }

    private void ensureAdminExists() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE username = 'admin'", Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.update("INSERT INTO t_user (username, password, phone, nickname, role) VALUES (?, ?, ?, ?, ?)",
                    "admin", "$2a$10$Zh0TR31oPWS2oEdx9mEfN.n2.4dq6sjbfEW0TTntRYGf0ueH7Wi62", "13800000000", "admin", "ADMIN");
                System.out.println("[Migration] 已创建管理员账号");
            }
        } catch (Exception e) {
            System.err.println("[Migration] 检查管理员账号失败: " + e.getMessage());
        }
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
