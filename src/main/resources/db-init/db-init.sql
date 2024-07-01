CREATE SCHEMA IF NOT EXISTS courseBot;

SET search_path TO courseBot;

CREATE TABLE IF NOT EXISTS "user" (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    role VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone_number VARCHAR(255),
    created_at timestamp
);

-- Create table authentication_principal
CREATE TABLE IF NOT EXISTS authentication_principal(
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    token VARCHAR(255),
    otp BIGINT,
    expires_at timestamp,
    chat_id BIGINT,
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES "user"(id)
);
