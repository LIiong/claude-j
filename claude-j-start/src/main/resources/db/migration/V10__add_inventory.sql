-- V10__add_inventory.sql
CREATE TABLE IF NOT EXISTS t_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id VARCHAR(64) NOT NULL COMMENT '库存唯一业务标识',
    product_id VARCHAR(64) NOT NULL COMMENT '关联商品ID',
    sku_code VARCHAR(32) NOT NULL COMMENT 'SKU编码',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存数',
    reserved_stock INT NOT NULL DEFAULT 0 COMMENT '预占库存数',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除'
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_id ON t_inventory(inventory_id);
CREATE INDEX IF NOT EXISTS idx_product_id ON t_inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_sku_code ON t_inventory(sku_code);