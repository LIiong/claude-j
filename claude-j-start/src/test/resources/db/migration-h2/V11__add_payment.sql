-- Payment DDL (H2 compatible)
CREATE TABLE IF NOT EXISTS t_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL UNIQUE,
    order_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    status VARCHAR(32) NOT NULL,
    method VARCHAR(32) NOT NULL,
    transaction_no VARCHAR(128),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payment_order_id ON t_payment(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_customer_id ON t_payment(customer_id);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_no ON t_payment(transaction_no);