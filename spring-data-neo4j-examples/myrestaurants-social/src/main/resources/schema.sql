CREATE TABLE restaurant (
        id BIGINT IDENTITY PRIMARY KEY,
        name VARCHAR(255),
        version BIGINT,
        zip_code VARCHAR(255),
        city VARCHAR(255),
        state VARCHAR(255)
    );