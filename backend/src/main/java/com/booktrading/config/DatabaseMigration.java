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
        jdbcTemplate.execute("SET NAMES utf8mb4");
        jdbcTemplate.execute("SET CHARACTER_SET_RESULTS = utf8mb4");

        // 建表（兼容 Railway 等无 init.sql 的环境）
        createTablesIfNotExist();

        // Schema migrations
        alterColumnIfShorter("t_operation_log", "method", "VARCHAR(200)");
        addColumnIfNotExists("t_comment", "parent_id", "BIGINT COMMENT '追评关联原评论ID'");
        addColumnIfNotExists("t_comment", "images_json", "TEXT COMMENT 'JSON数组，存储评价图片路径'");
        addColumnIfNotExists("t_order", "logistics_company", "VARCHAR(50)");
        addColumnIfNotExists("t_order", "tracking_number", "VARCHAR(100)");
        addColumnIfNotExists("t_order", "return_logistics_company", "VARCHAR(50)");
        addColumnIfNotExists("t_order", "return_tracking_number", "VARCHAR(100)");
        addColumnIfNotExists("t_order", "dispute_images", "TEXT COMMENT '纠纷证据图片JSON'");

        // 初始数据
        ensureAdminExists();
        ensureCategoriesExist();
    }

    private void createTablesIfNotExist() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS t_user (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, phone VARCHAR(20), email VARCHAR(100), " +
                "avatar VARCHAR(255), nickname VARCHAR(50), role VARCHAR(20) NOT NULL DEFAULT 'STUDENT', " +
                "reputation_score DECIMAL(3,1) DEFAULT 5.0, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_username(username), INDEX idx_phone(phone), INDEX idx_email(email)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_category (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50) NOT NULL, icon VARCHAR(255), " +
                "sort_order INT DEFAULT 0, status INT DEFAULT 1, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_book (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(200) NOT NULL, author VARCHAR(100), " +
                "publisher VARCHAR(100), isbn VARCHAR(30), original_price DECIMAL(10,2), " +
                "selling_price DECIMAL(10,2) NOT NULL, book_condition VARCHAR(20) DEFAULT 'GOOD', " +
                "description TEXT, cover_image VARCHAR(255), images_json TEXT, category_id BIGINT, " +
                "seller_id BIGINT NOT NULL, quantity INT DEFAULT 1, " +
                "status VARCHAR(20) DEFAULT 'ON_SALE', reject_reason VARCHAR(500), " +
                "view_count INT DEFAULT 0, want_count INT DEFAULT 0, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_seller(seller_id), INDEX idx_category(category_id), INDEX idx_status(status), " +
                "FULLTEXT INDEX ft_title(title)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_order (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, order_no VARCHAR(50) NOT NULL UNIQUE, " +
                "book_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, status VARCHAR(20) DEFAULT 'PENDING', " +
                "address VARCHAR(500), phone VARCHAR(20), receiver_name VARCHAR(50), " +
                "shipping_info VARCHAR(500), logistics_company VARCHAR(50), tracking_number VARCHAR(100), " +
                "return_logistics_company VARCHAR(50), return_tracking_number VARCHAR(100), " +
                "refund_reason VARCHAR(500), dispute_reason VARCHAR(500), dispute_images TEXT, " +
                "admin_note VARCHAR(500), " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_buyer(buyer_id), INDEX idx_seller(seller_id), " +
                "INDEX idx_book(book_id), INDEX idx_order_no(order_no), INDEX idx_status(status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_interaction (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, book_id BIGINT NOT NULL, " +
                "type VARCHAR(20) NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_user_type(user_id, type), INDEX idx_book(book_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_comment (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, parent_id BIGINT, " +
                "book_id BIGINT NOT NULL, user_id BIGINT NOT NULL, content TEXT, " +
                "rating INT NOT NULL, images_json TEXT, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_book(book_id), INDEX idx_user(user_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_message (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, sender_id BIGINT, receiver_id BIGINT NOT NULL, " +
                "content TEXT NOT NULL, type VARCHAR(20) DEFAULT 'SYSTEM', is_read INT DEFAULT 0, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_receiver(receiver_id), INDEX idx_is_read(is_read), " +
                "INDEX idx_sender_receiver(sender_id, receiver_id, create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_operation_log (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT, username VARCHAR(50), " +
                "module VARCHAR(50), action VARCHAR(50), detail TEXT, ip VARCHAR(50), method VARCHAR(200), " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_user(user_id), INDEX idx_create_time(create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_address (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, " +
                "receiver_name VARCHAR(50) NOT NULL, phone VARCHAR(20) NOT NULL, " +
                "province VARCHAR(50), city VARCHAR(50), district VARCHAR(50), " +
                "detail VARCHAR(200) NOT NULL, is_default INT DEFAULT 0, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_user(user_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_payment (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, " +
                "amount DECIMAL(10,2) NOT NULL, method VARCHAR(20) DEFAULT 'ALIPAY', " +
                "status VARCHAR(20) DEFAULT 'PENDING', transaction_no VARCHAR(100), alipay_trade_no VARCHAR(100), " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_order(order_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_exchange (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, initiator_book_id BIGINT NOT NULL, " +
                "target_book_id BIGINT NOT NULL, initiator_id BIGINT NOT NULL, target_user_id BIGINT NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'PENDING', message VARCHAR(500), " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_initiator(initiator_id), INDEX idx_target(target_user_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_banner (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, book_id BIGINT NOT NULL, " +
                "title VARCHAR(100), image_url VARCHAR(500), sort_order INT DEFAULT 0, " +
                "status TINYINT DEFAULT 1, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted TINYINT DEFAULT 0, INDEX idx_status(status), INDEX idx_sort(sort_order)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",

            "CREATE TABLE IF NOT EXISTS t_chat_session (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, user1_id BIGINT NOT NULL, user2_id BIGINT NOT NULL, " +
                "last_message VARCHAR(500), last_message_time DATETIME, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "deleted INT DEFAULT 0, INDEX idx_user1(user1_id), INDEX idx_user2(user2_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        };

        for (String sql : ddl) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                // 表已存在则忽略
                if (e.getMessage() != null && !e.getMessage().contains("already exists")) {
                    System.err.println("[Migration] 建表失败: " + e.getMessage());
                }
            }
        }
        System.out.println("[Migration] 表结构检查完成");
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
                insertDefaultCategories();
            } else {
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
        int suspicious = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0xC0 && c <= 0xFF) {
                suspicious++;
            }
        }
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
