-- ============================================================
-- 多项目卡密授权中心 - 完整数据库迁移脚本
-- 版本: 2.0
-- 日期: 2026-06-21
-- 说明: 本脚本在原有 kami.sql 基础上进行完整的多项目改造
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 创建 projects 表 (项目管理)
-- ============================================================
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_name VARCHAR(100) NOT NULL COMMENT '项目名称',
    project_code VARCHAR(80) NOT NULL COMMENT '项目标识',
    project_token VARCHAR(7) NOT NULL COMMENT '项目7位随机访问码',
    project_type VARCHAR(30) NOT NULL DEFAULT 'other' COMMENT '项目类型: website/api/windows/android/ios/mini_program/wordpress/other',
    environment VARCHAR(30) NOT NULL DEFAULT 'production' COMMENT '环境: sandbox/production',
    usage_mode VARCHAR(30) NOT NULL DEFAULT 'direct_license' COMMENT '卡密使用模式: direct_license/redeem_to_account',
    status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态: enabled/disabled',
    remark VARCHAR(500) NULL COMMENT '备注',
    enable_device_bind TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用设备绑定',
    device_bind_mode VARCHAR(30) NOT NULL DEFAULT 'none' COMMENT '设备绑定模式: none/machine_code/android_id/ios_id/custom',
    enable_signature TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用API签名认证',
    enable_ip_whitelist TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用IP白名单',
    rate_limit_per_minute INT NOT NULL DEFAULT 120 COMMENT '每分钟请求限制',
    max_generate_per_request INT NOT NULL DEFAULT 100 COMMENT '单次API最大发卡数量',
    max_generate_per_day INT NOT NULL DEFAULT 10000 COMMENT '每日API最大发卡数量',
    webhook_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用WebHook',
    webhook_url VARCHAR(500) NULL COMMENT 'WebHook地址',
    webhook_secret VARCHAR(255) NULL COMMENT 'WebHook签名密钥',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_projects_code (project_code),
    UNIQUE KEY uk_projects_token (project_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ============================================================
-- 2. 创建 card_packages 表 (套餐模板)
-- ============================================================
CREATE TABLE IF NOT EXISTS card_packages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    package_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    package_code VARCHAR(50) NOT NULL COMMENT '套餐标识',
    card_type VARCHAR(20) NOT NULL COMMENT '卡密类型: count/time/hybrid/permanent',
    count_value INT NOT NULL DEFAULT 0 COMMENT '次数值(次数卡专用)',
    duration_days INT NOT NULL DEFAULT 0 COMMENT '时长天数(期限卡专用)',
    is_permanent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否永久卡',
    price DECIMAL(10, 2) NULL COMMENT '价格',
    status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态: enabled/disabled',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_package_project_code (project_id, package_code),
    KEY idx_package_project_status (project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐模板表';

-- ============================================================
-- 3. 创建 project_api_keys 表 (项目API密钥)
-- ============================================================
CREATE TABLE IF NOT EXISTS project_api_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    key_name VARCHAR(100) NOT NULL COMMENT 'API Key名称',
    api_key VARCHAR(64) NOT NULL COMMENT 'API Key',
    api_secret VARCHAR(128) NOT NULL COMMENT 'API Secret(创建时生成,仅展示一次)',
    api_secret_hash VARCHAR(255) NOT NULL COMMENT 'API Secret哈希值',
    permissions TEXT NULL COMMENT '权限列表JSON',
    allowed_ips TEXT NULL COMMENT 'IP白名单(逗号分隔)',
    status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态: enabled/disabled',
    environment VARCHAR(30) NOT NULL DEFAULT 'production' COMMENT '环境: sandbox/production',
    last_used_at DATETIME NULL COMMENT '最后使用时间',
    use_count BIGINT NOT NULL DEFAULT 0 COMMENT '使用次数',
    expired_at DATETIME NULL COMMENT '过期时间',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_key (api_key),
    KEY idx_project_api_key (project_id, api_key),
    KEY idx_api_key_status (api_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目API密钥表';

-- ============================================================
-- 4. 修改 cards 表 (添加多项目字段)
-- ============================================================
ALTER TABLE cards ADD COLUMN IF NOT EXISTS project_id BIGINT NULL COMMENT '所属项目ID';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS package_id BIGINT NULL COMMENT '套餐ID';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS order_no VARCHAR(100) NULL COMMENT '订单号';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS source VARCHAR(50) NOT NULL DEFAULT 'admin' COMMENT '来源: admin/api/shop/external';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS bind_device_id VARCHAR(255) NULL COMMENT '绑定设备ID';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS bind_time DATETIME NULL COMMENT '绑定时间';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS bind_type VARCHAR(30) NULL COMMENT '绑定类型';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS redeemed_user_id VARCHAR(100) NULL COMMENT '兑换用户ID';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS redeemed_at DATETIME NULL COMMENT '兑换时间';
ALTER TABLE cards ADD COLUMN IF NOT EXISTS activated_at DATETIME NULL COMMENT '激活时间';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_cards_project_key ON cards(project_id, card_key(100));
CREATE INDEX IF NOT EXISTS idx_cards_project_status ON cards(project_id, status);
CREATE INDEX IF NOT EXISTS idx_cards_package ON cards(package_id);
CREATE INDEX IF NOT EXISTS idx_cards_order ON cards(order_no);
CREATE INDEX IF NOT EXISTS idx_cards_bind_device ON cards(bind_device_id);
CREATE INDEX IF NOT EXISTS idx_cards_redeemed_user ON cards(redeemed_user_id);

-- ============================================================
-- 5. 创建 card_issue_orders 表 (发卡订单)
-- ============================================================
CREATE TABLE IF NOT EXISTS card_issue_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    api_key_id BIGINT NULL COMMENT 'API Key ID',
    order_no VARCHAR(100) NOT NULL COMMENT '订单号',
    package_id BIGINT NULL COMMENT '套餐ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '生成数量',
    source VARCHAR(50) NOT NULL DEFAULT 'api' COMMENT '来源: api/admin/external',
    status VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '状态: success/failed',
    request_body TEXT NULL COMMENT '请求体',
    created_cards TEXT NULL COMMENT '生成的卡密列表(JSON)',
    error_message VARCHAR(500) NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_issue_project_order (project_id, order_no),
    KEY idx_issue_project_time (project_id, created_at),
    KEY idx_issue_api_key (api_key_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发卡订单表';

-- ============================================================
-- 6. 创建 api_call_logs 表 (API调用日志)
-- ============================================================
CREATE TABLE IF NOT EXISTS api_call_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    api_key_id BIGINT NULL COMMENT 'API Key ID',
    api_key VARCHAR(64) NULL COMMENT 'API Key快照',
    endpoint VARCHAR(255) NOT NULL COMMENT '接口路径',
    method VARCHAR(20) NOT NULL COMMENT 'HTTP方法',
    request_ip VARCHAR(64) NULL COMMENT '请求IP',
    request_body TEXT NULL COMMENT '请求体',
    response_code INT NULL COMMENT '响应状态码',
    response_message VARCHAR(500) NULL COMMENT '响应消息',
    success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功',
    cost_ms BIGINT NOT NULL DEFAULT 0 COMMENT '耗时(毫秒)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_api_call_project_time (project_id, created_at),
    KEY idx_api_call_api_key_time (api_key_id, created_at),
    KEY idx_api_call_endpoint (endpoint),
    KEY idx_api_call_ip (request_ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API调用日志表';

-- ============================================================
-- 7. 创建 card_consume_logs 表 (卡密消费日志)
-- ============================================================
CREATE TABLE IF NOT EXISTS card_consume_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    card_id BIGINT NOT NULL COMMENT '卡密ID',
    card_key VARCHAR(512) NULL COMMENT '卡密快照',
    biz_id VARCHAR(100) NOT NULL COMMENT '业务ID(幂等)',
    consume_count INT NOT NULL DEFAULT 1 COMMENT '消费次数',
    before_count INT NULL COMMENT '消费前剩余次数',
    after_count INT NULL COMMENT '消费后剩余次数',
    consume_scene VARCHAR(100) NULL COMMENT '消费场景',
    remark VARCHAR(500) NULL COMMENT '备注',
    refunded TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已返还',
    refund_time DATETIME NULL COMMENT '返还时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consume_project_card_biz (project_id, card_id, biz_id),
    KEY idx_consume_project_time (project_id, created_at),
    KEY idx_consume_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡密消费日志表';

-- ============================================================
-- 8. 创建 card_redeem_logs 表 (卡密兑换日志)
-- ============================================================
CREATE TABLE IF NOT EXISTS card_redeem_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    card_id BIGINT NOT NULL COMMENT '卡密ID',
    card_key VARCHAR(512) NULL COMMENT '卡密快照',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    entitlement_id BIGINT NULL COMMENT '用户权益ID',
    card_type VARCHAR(20) NULL COMMENT '卡密类型',
    count_value INT NULL COMMENT '兑换次数',
    duration_days INT NULL COMMENT '兑换天数',
    before_count INT NULL COMMENT '兑换前剩余次数',
    after_count INT NULL COMMENT '兑换后剩余次数',
    before_expire DATETIME NULL COMMENT '兑换前到期时间',
    after_expire DATETIME NULL COMMENT '兑换后到期时间',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_redeem_project_time (project_id, created_at),
    KEY idx_redeem_card (card_id),
    KEY idx_redeem_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡密兑换日志表';

-- ============================================================
-- 9. 创建 device_bind_logs 表 (设备绑定日志)
-- ============================================================
CREATE TABLE IF NOT EXISTS device_bind_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    card_id BIGINT NOT NULL COMMENT '卡密ID',
    card_key VARCHAR(512) NULL COMMENT '卡密快照',
    device_id VARCHAR(255) NOT NULL COMMENT '设备ID',
    bind_type VARCHAR(30) NULL COMMENT '绑定类型',
    action VARCHAR(20) NOT NULL COMMENT '操作: bind/unbind',
    operator VARCHAR(50) NULL COMMENT '操作人',
    reason VARCHAR(500) NULL COMMENT '原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_device_bind_project_time (project_id, created_at),
    KEY idx_device_bind_card (card_id),
    KEY idx_device_bind_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备绑定日志表';

-- ============================================================
-- 10. 创建 user_entitlements 表 (用户权益)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_entitlements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    entitlement_type VARCHAR(20) NOT NULL DEFAULT 'count' COMMENT '权益类型: count/time/hybrid/permanent',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总次数',
    remaining_count INT NOT NULL DEFAULT 0 COMMENT '剩余次数',
    start_time DATETIME NULL COMMENT '开始时间',
    expire_time DATETIME NULL COMMENT '到期时间',
    is_permanent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否永久',
    source_card_id BIGINT NULL COMMENT '来源卡密ID',
    source_order_no VARCHAR(100) NULL COMMENT '来源订单号',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/expired/used_up/disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entitlement_project_user (project_id, user_id),
    KEY idx_entitlement_project_expire (project_id, expire_time),
    KEY idx_entitlement_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益表';

-- ============================================================
-- 11. 创建 entitlement_consume_logs 表 (用户权益消费日志)
-- ============================================================
CREATE TABLE IF NOT EXISTS entitlement_consume_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    entitlement_id BIGINT NOT NULL COMMENT '权益ID',
    biz_id VARCHAR(100) NOT NULL COMMENT '业务ID(幂等)',
    consume_count INT NOT NULL DEFAULT 1 COMMENT '消费次数',
    before_count INT NULL COMMENT '消费前剩余次数',
    after_count INT NULL COMMENT '消费后剩余次数',
    consume_scene VARCHAR(100) NULL COMMENT '消费场景',
    remark VARCHAR(500) NULL COMMENT '备注',
    refunded TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已返还',
    refund_time DATETIME NULL COMMENT '返还时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entitlement_consume_project_user_biz (project_id, user_id, biz_id),
    KEY idx_entitlement_consume_project_time (project_id, created_at),
    KEY idx_entitlement_consume_user (user_id),
    KEY idx_entitlement_consume_entitlement (entitlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益消费日志表';

-- ============================================================
-- 12. 创建 api_nonces 表 (API防重放)
-- ============================================================
CREATE TABLE IF NOT EXISTS api_nonces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    api_key_id BIGINT NOT NULL COMMENT 'API Key ID',
    nonce VARCHAR(120) NOT NULL COMMENT '随机数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_nonce_project_key (project_id, api_key_id, nonce),
    KEY idx_nonce_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API防重放表';

-- ============================================================
-- 13. 创建 webhook_logs 表 (WebHook日志)
-- ============================================================
CREATE TABLE IF NOT EXISTS webhook_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    event_data TEXT NULL COMMENT '事件数据JSON',
    webhook_url VARCHAR(500) NOT NULL COMMENT 'WebHook地址',
    request_body TEXT NULL COMMENT '请求体',
    response_code INT NULL COMMENT '响应状态码',
    response_body TEXT NULL COMMENT '响应体',
    success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(500) NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_webhook_project_time (project_id, created_at),
    KEY idx_webhook_event (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WebHook日志表';

-- ============================================================
-- 14. 修改 orders 表 (添加多项目字段)
-- ============================================================
ALTER TABLE orders ADD COLUMN IF NOT EXISTS project_id BIGINT NULL COMMENT '项目ID';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS source VARCHAR(50) NOT NULL DEFAULT 'admin' COMMENT '来源: admin/api/shop/external';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS external_order_no VARCHAR(128) NULL COMMENT '外部订单号';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS package_id BIGINT NULL COMMENT '套餐ID';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS card_keys TEXT NULL COMMENT '生成的卡密列表';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_orders_project_order ON orders(project_id, order_no);
CREATE INDEX IF NOT EXISTS idx_orders_project_external ON orders(project_id, external_order_no);

-- ============================================================
-- 15. 修改 api_keys 表 (添加多项目字段)
-- ============================================================
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS project_id BIGINT NULL COMMENT '项目ID';
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS api_secret_hash VARCHAR(255) NULL COMMENT 'API Secret哈希';
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS permissions TEXT NULL COMMENT '权限列表';
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS allowed_ips TEXT NULL COMMENT 'IP白名单';
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS environment VARCHAR(30) NOT NULL DEFAULT 'production' COMMENT '环境';
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS expired_at DATETIME NULL COMMENT '过期时间';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_api_keys_project_key ON api_keys(project_id, api_key);

-- ============================================================
-- 16. 修改 card_pricing 表 (添加项目ID)
-- ============================================================
ALTER TABLE card_pricing ADD COLUMN IF NOT EXISTS project_id BIGINT NULL COMMENT '项目ID';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_pricing_project_type ON card_pricing(project_id, type);

-- ============================================================
-- 17. 修改 operation_logs 表 (添加项目ID)
-- ============================================================
ALTER TABLE operation_logs ADD COLUMN IF NOT EXISTS project_id BIGINT NULL COMMENT '项目ID';

-- ============================================================
-- 18. 数据迁移 - 创建默认项目
-- ============================================================

-- 生成7位随机码的函数已通过代码实现,这里使用临时值
INSERT INTO projects (
    project_name, project_code, project_token, project_type, environment, usage_mode,
    status, remark, enable_device_bind, device_bind_mode
)
SELECT 'Default Project', 'default', 'DEFAULT', 'other', 'production', 'direct_license',
       'enabled', 'Migrated from the original single-project data', 0, 'none'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'default');

-- 获取默认项目ID
SET @default_project_id := (SELECT id FROM projects WHERE project_code = 'default' LIMIT 1);

-- 迁移旧数据到默认项目
UPDATE cards SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE card_pricing SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE api_keys SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE orders SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE operation_logs SET project_id = @default_project_id WHERE project_id IS NULL;

-- ============================================================
-- 19. 清理定时任务 - 删除过期的 nonce 和临时数据
-- ============================================================
-- 建议在应用层实现定时任务,每小时清理5分钟前的 nonce
-- DELETE FROM api_nonces WHERE created_at < DATE_SUB(NOW(), INTERVAL 5 MINUTE);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 迁移完成提示
-- ============================================================
-- 注意事项:
-- 1. default 项目的 project_token = 'DEFAULT' 需要通过后端代码修改为7位随机码
-- 2. 建议在应用启动时执行 project_token 修复逻辑
-- 3. 旧的 api_keys 表字段保留,如需清理请手动执行
-- 4. 建议备份数据库后再执行此脚本
-- ============================================================
