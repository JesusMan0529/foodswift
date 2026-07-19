-- =============================================
-- 苍穹外卖 数据库设计
-- 生成日期: 2026-07-19
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ssd DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ssd;

-- =============================================
-- 1. employee - 员工表
-- 用于存储商家内部的员工信息
-- =============================================
DROP TABLE IF EXISTS employee;
CREATE TABLE employee (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)  DEFAULT NULL COMMENT '姓名',
    username    VARCHAR(32)  DEFAULT NULL COMMENT '用户名',
    password    VARCHAR(64)  DEFAULT NULL COMMENT '密码',
    phone       VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    sex         VARCHAR(2)   DEFAULT NULL COMMENT '性别',
    id_number   VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    status      INT          DEFAULT 1 COMMENT '账号状态: 1正常 0锁定',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '最后修改时间',
    create_user BIGINT       DEFAULT NULL COMMENT '创建人id',
    update_user BIGINT       DEFAULT NULL COMMENT '最后修改人id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工表';

-- =============================================
-- 2. category - 分类表
-- 用于存储商品的分类信息
-- =============================================
DROP TABLE IF EXISTS category;
CREATE TABLE category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)  DEFAULT NULL COMMENT '分类名称',
    type        INT          DEFAULT NULL COMMENT '分类类型: 1菜品分类 2套餐分类',
    sort        INT          DEFAULT NULL COMMENT '排序字段,用于分类数据的排序',
    status      INT          DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '最后修改时间',
    create_user BIGINT       DEFAULT NULL COMMENT '创建人id',
    update_user BIGINT       DEFAULT NULL COMMENT '最后修改人id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类表';

-- =============================================
-- 3. dish - 菜品表
-- 用于存储菜品的信息
-- =============================================
DROP TABLE IF EXISTS dish;
CREATE TABLE dish (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)    DEFAULT NULL COMMENT '菜品名称',
    category_id BIGINT         DEFAULT NULL COMMENT '分类id(逻辑外键)',
    price       DECIMAL(10,2)  DEFAULT NULL COMMENT '菜品价格',
    image       VARCHAR(255)   DEFAULT NULL COMMENT '图片路径',
    description VARCHAR(255)   DEFAULT NULL COMMENT '菜品描述',
    status      INT            DEFAULT 1 COMMENT '售卖状态: 1起售 0停售',
    create_time DATETIME       DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME       DEFAULT NULL COMMENT '最后修改时间',
    create_user BIGINT         DEFAULT NULL COMMENT '创建人id',
    update_user BIGINT         DEFAULT NULL COMMENT '最后修改人id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品表';

-- =============================================
-- 4. dish_flavor - 菜品口味表
-- 用于存储菜品的口味信息
-- =============================================
DROP TABLE IF EXISTS dish_flavor;
CREATE TABLE dish_flavor (
    id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dish_id BIGINT       DEFAULT NULL COMMENT '菜品id(逻辑外键)',
    name    VARCHAR(32)  DEFAULT NULL COMMENT '口味名称',
    value   VARCHAR(255) DEFAULT NULL COMMENT '口味值',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品口味表';

-- =============================================
-- 5. setmeal - 套餐表
-- 用于存储套餐的信息
-- =============================================
DROP TABLE IF EXISTS setmeal;
CREATE TABLE setmeal (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)    DEFAULT NULL COMMENT '套餐名称',
    category_id BIGINT         DEFAULT NULL COMMENT '分类id(逻辑外键)',
    price       DECIMAL(10,2)  DEFAULT NULL COMMENT '套餐价格',
    image       VARCHAR(255)   DEFAULT NULL COMMENT '图片路径',
    description VARCHAR(255)   DEFAULT NULL COMMENT '套餐描述',
    status      INT            DEFAULT 1 COMMENT '售卖状态: 1起售 0停售',
    create_time DATETIME       DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME       DEFAULT NULL COMMENT '最后修改时间',
    create_user BIGINT         DEFAULT NULL COMMENT '创建人id',
    update_user BIGINT         DEFAULT NULL COMMENT '最后修改人id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='套餐表';

-- =============================================
-- 6. setmeal_dish - 套餐菜品关系表
-- 用于存储套餐和菜品的关联关系
-- =============================================
DROP TABLE IF EXISTS setmeal_dish;
CREATE TABLE setmeal_dish (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    setmeal_id BIGINT         DEFAULT NULL COMMENT '套餐id(逻辑外键)',
    dish_id    BIGINT         DEFAULT NULL COMMENT '菜品id(逻辑外键)',
    name       VARCHAR(32)    DEFAULT NULL COMMENT '菜品名称(冗余字段)',
    price      DECIMAL(10,2)  DEFAULT NULL COMMENT '菜品单价(冗余字段)',
    copies     INT            DEFAULT NULL COMMENT '菜品份数',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='套餐菜品关系表';

-- =============================================
-- 7. user - 用户表(C端)
-- 用于存储C端用户的信息
-- =============================================
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    openid      VARCHAR(45)  DEFAULT NULL COMMENT '微信用户的唯一标识',
    name        VARCHAR(32)  DEFAULT NULL COMMENT '用户姓名',
    phone       VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    sex         VARCHAR(2)   DEFAULT NULL COMMENT '性别',
    id_number   VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    avatar      VARCHAR(500) DEFAULT NULL COMMENT '微信用户头像路径',
    create_time DATETIME     DEFAULT NULL COMMENT '注册时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- =============================================
-- 8. address_book - 地址表
-- 用于存储C端用户的收货地址信息
-- =============================================
DROP TABLE IF EXISTS address_book;
CREATE TABLE address_book (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       DEFAULT NULL COMMENT '用户id(逻辑外键)',
    consignee     VARCHAR(50)  DEFAULT NULL COMMENT '收货人',
    sex           VARCHAR(2)   DEFAULT NULL COMMENT '性别',
    phone         VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    province_code VARCHAR(12)  DEFAULT NULL COMMENT '省份编码',
    province_name VARCHAR(32)  DEFAULT NULL COMMENT '省份名称',
    city_code     VARCHAR(12)  DEFAULT NULL COMMENT '城市编码',
    city_name     VARCHAR(32)  DEFAULT NULL COMMENT '城市名称',
    district_code VARCHAR(12)  DEFAULT NULL COMMENT '区县编码',
    district_name VARCHAR(32)  DEFAULT NULL COMMENT '区县名称',
    detail        VARCHAR(200) DEFAULT NULL COMMENT '详细地址信息(具体到门牌号)',
    label         VARCHAR(100) DEFAULT NULL COMMENT '标签(公司、家、学校)',
    is_default    TINYINT(1)   DEFAULT 0 COMMENT '是否默认地址: 1是 0否',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='地址表';

-- =============================================
-- 9. shopping_cart - 购物车表
-- 用于存储C端用户的购物车信息
-- =============================================
DROP TABLE IF EXISTS shopping_cart;
CREATE TABLE shopping_cart (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)    DEFAULT NULL COMMENT '商品名称',
    image       VARCHAR(255)   DEFAULT NULL COMMENT '商品图片路径',
    user_id     BIGINT         DEFAULT NULL COMMENT '用户id(逻辑外键)',
    dish_id     BIGINT         DEFAULT NULL COMMENT '菜品id(逻辑外键)',
    setmeal_id  BIGINT         DEFAULT NULL COMMENT '套餐id(逻辑外键)',
    dish_flavor VARCHAR(50)    DEFAULT NULL COMMENT '菜品口味',
    number      INT            DEFAULT NULL COMMENT '商品数量',
    amount      DECIMAL(10,2)  DEFAULT NULL COMMENT '商品单价',
    create_time DATETIME       DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- =============================================
-- 10. orders - 订单表
-- 用于存储C端用户的订单数据
-- =============================================
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    number                  VARCHAR(50)    DEFAULT NULL COMMENT '订单号',
    status                  INT            DEFAULT NULL COMMENT '订单状态: 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消',
    user_id                 BIGINT         DEFAULT NULL COMMENT '用户id(逻辑外键)',
    address_book_id         BIGINT         DEFAULT NULL COMMENT '地址id(逻辑外键)',
    order_time              DATETIME       DEFAULT NULL COMMENT '下单时间',
    checkout_time           DATETIME       DEFAULT NULL COMMENT '付款时间',
    pay_method              INT            DEFAULT NULL COMMENT '支付方式: 1微信支付 2支付宝支付',
    pay_status              TINYINT        DEFAULT 0 COMMENT '支付状态: 0未支付 1已支付 2退款',
    amount                  DECIMAL(10,2)  DEFAULT NULL COMMENT '订单金额',
    remark                  VARCHAR(100)   DEFAULT NULL COMMENT '备注信息',
    phone                   VARCHAR(11)    DEFAULT NULL COMMENT '手机号',
    address                 VARCHAR(255)   DEFAULT NULL COMMENT '详细地址信息',
    user_name               VARCHAR(32)    DEFAULT NULL COMMENT '用户姓名',
    consignee               VARCHAR(32)    DEFAULT NULL COMMENT '收货人',
    cancel_reason           VARCHAR(255)   DEFAULT NULL COMMENT '订单取消原因',
    rejection_reason        VARCHAR(255)   DEFAULT NULL COMMENT '拒单原因',
    cancel_time             DATETIME       DEFAULT NULL COMMENT '订单取消时间',
    estimated_delivery_time DATETIME       DEFAULT NULL COMMENT '预计送达时间',
    delivery_status         TINYINT        DEFAULT NULL COMMENT '配送状态: 1立即送出 0选择具体时间',
    delivery_time           DATETIME       DEFAULT NULL COMMENT '送达时间',
    pack_amount             INT            DEFAULT NULL COMMENT '打包费',
    tableware_number        INT            DEFAULT NULL COMMENT '餐具数量',
    tableware_status        TINYINT        DEFAULT NULL COMMENT '餐具数量状态: 1按餐量提供 0选择具体数量',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- =============================================
-- 11. order_detail - 订单明细表
-- 用于存储C端用户的订单明细数据
-- =============================================
DROP TABLE IF EXISTS order_detail;
CREATE TABLE order_detail (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)    DEFAULT NULL COMMENT '商品名称',
    image       VARCHAR(255)   DEFAULT NULL COMMENT '商品图片路径',
    order_id    BIGINT         DEFAULT NULL COMMENT '订单id(逻辑外键)',
    dish_id     BIGINT         DEFAULT NULL COMMENT '菜品id(逻辑外键)',
    setmeal_id  BIGINT         DEFAULT NULL COMMENT '套餐id(逻辑外键)',
    dish_flavor VARCHAR(50)    DEFAULT NULL COMMENT '菜品口味',
    number      INT            DEFAULT NULL COMMENT '商品数量',
    amount      DECIMAL(10,2)  DEFAULT NULL COMMENT '商品单价',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';
