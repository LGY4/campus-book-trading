CREATE DATABASE IF NOT EXISTS book_trading DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE book_trading;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar VARCHAR(255),
    nickname VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/ADMIN',
    reputation_score DECIMAL(3,1) DEFAULT 5.0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/BANNED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 书籍类别表
CREATE TABLE IF NOT EXISTS t_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(255),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1 COMMENT '1=启用 0=禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 书籍表
CREATE TABLE IF NOT EXISTS t_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    publisher VARCHAR(100),
    isbn VARCHAR(30),
    original_price DECIMAL(10,2),
    selling_price DECIMAL(10,2) NOT NULL,
    book_condition VARCHAR(20) DEFAULT 'GOOD' COMMENT 'NEW/GOOD/FAIR/POOR',
    description TEXT,
    cover_image VARCHAR(255),
    images_json TEXT COMMENT 'JSON数组，存储多张图片路径',
    category_id BIGINT,
    seller_id BIGINT NOT NULL,
    quantity INT DEFAULT 1 COMMENT '库存数量',
    status VARCHAR(20) DEFAULT 'ON_SALE' COMMENT 'ON_SALE/SOLD/OFF_SHELF/PENDING/REJECTED',
    reject_reason VARCHAR(500) COMMENT '审核驳回原因',
    view_count INT DEFAULT 0,
    want_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_seller (seller_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_title (title),
    FOREIGN KEY (category_id) REFERENCES t_category(id),
    FOREIGN KEY (seller_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    book_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDING/RETURNING/REFUNDED/DISPUTE',
    address VARCHAR(500),
    phone VARCHAR(20),
    receiver_name VARCHAR(50),
    shipping_info VARCHAR(500),
    logistics_company VARCHAR(50),
    tracking_number VARCHAR(100),
    return_logistics_company VARCHAR(50),
    return_tracking_number VARCHAR(100),
    refund_reason VARCHAR(500),
    dispute_reason VARCHAR(500),
    dispute_images TEXT COMMENT '纠纷证据图片JSON',
    admin_note VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_book (book_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    FOREIGN KEY (book_id) REFERENCES t_book(id),
    FOREIGN KEY (buyer_id) REFERENCES t_user(id),
    FOREIGN KEY (seller_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 互动行为表
CREATE TABLE IF NOT EXISTS t_interaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'VIEW/FAVORITE/WANT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user_type (user_id, type),
    INDEX idx_book (book_id),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (book_id) REFERENCES t_book(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表
CREATE TABLE IF NOT EXISTS t_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    parent_id BIGINT COMMENT '追评关联原评论ID',
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    rating INT NOT NULL COMMENT '1-5星',
    images_json TEXT COMMENT 'JSON数组，存储评价图片路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_book (book_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (order_id) REFERENCES t_order(id),
    FOREIGN KEY (book_id) REFERENCES t_book(id),
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS t_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'SYSTEM' COMMENT 'SYSTEM/TRADE/CHAT',
    is_read INT DEFAULT 0 COMMENT '0=未读 1=已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_receiver (receiver_id),
    INDEX idx_is_read (is_read),
    INDEX idx_sender_receiver (sender_id, receiver_id, create_time),
    FOREIGN KEY (receiver_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(50),
    action VARCHAR(50),
    detail TEXT,
    ip VARCHAR(50),
    method VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收货地址表
CREATE TABLE IF NOT EXISTS t_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    detail VARCHAR(200) NOT NULL,
    is_default INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 支付记录表
CREATE TABLE IF NOT EXISTS t_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) DEFAULT 'ALIPAY' COMMENT 'ALIPAY/BALANCE',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/REFUNDED',
    transaction_no VARCHAR(100),
    alipay_trade_no VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_order (order_id),
    FOREIGN KEY (order_id) REFERENCES t_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 以书换书表
CREATE TABLE IF NOT EXISTS t_exchange (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    initiator_book_id BIGINT NOT NULL,
    target_book_id BIGINT NOT NULL,
    initiator_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/COMPLETED',
    message VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_initiator (initiator_id),
    INDEX idx_target (target_user_id),
    INDEX idx_books (initiator_book_id, target_book_id),
    FOREIGN KEY (initiator_book_id) REFERENCES t_book(id),
    FOREIGN KEY (target_book_id) REFERENCES t_book(id),
    FOREIGN KEY (initiator_id) REFERENCES t_user(id),
    FOREIGN KEY (target_user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 轮播推荐表
CREATE TABLE IF NOT EXISTS t_banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_id BIGINT NOT NULL COMMENT '关联书籍ID',
    title VARCHAR(100) COMMENT '展示标题（可选，默认用书名）',
    image_url VARCHAR(500) COMMENT '展示图片（可选，默认用封面）',
    sort_order INT DEFAULT 0 COMMENT '排序值，越大越靠前',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_sort (sort_order),
    FOREIGN KEY (book_id) REFERENCES t_book(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 聊天会话表
CREATE TABLE IF NOT EXISTS t_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    last_message VARCHAR(500),
    last_message_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    FOREIGN KEY (user1_id) REFERENCES t_user(id),
    FOREIGN KEY (user2_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始数据由应用启动时通过 DatabaseMigration 类插入（解决 Docker 环境中文编码问题）
