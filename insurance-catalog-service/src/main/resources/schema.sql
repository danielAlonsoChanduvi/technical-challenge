CREATE TABLE insuranceplan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    base_price DOUBLE NOT NULL,
    min_age INT,
    max_age INT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
