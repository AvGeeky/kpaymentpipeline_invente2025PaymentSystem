-- DROP TABLES (reverse order to handle FKs safely)
DROP TABLE IF EXISTS slots CASCADE;
DROP TABLE IF EXISTS pass CASCADE;
DROP TABLE IF EXISTS receipts CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS events CASCADE;

-- USERS TABLE
CREATE TABLE users (
                       email TEXT PRIMARY KEY,
                       name TEXT NOT NULL,
                       phone TEXT,
                       created_at TIMESTAMPTZ DEFAULT now(),
                       status_paid BOOLEAN DEFAULT FALSE
);

-- RECEIPTS TABLE
CREATE TABLE receipts (
                          payment_id VARCHAR(100) PRIMARY KEY,
                          paid_on TIMESTAMP NOT NULL,
                          method VARCHAR(50),
                          email VARCHAR(255) REFERENCES users(email),
                          phone VARCHAR(20)
);

-- EVENTS TABLE
CREATE TABLE events (
                        id SERIAL PRIMARY KEY,
                        name TEXT NOT NULL
);

-- PASS TABLE
CREATE TABLE pass (
                      uuid text PRIMARY KEY,
                      user_email TEXT REFERENCES users(email),
                      payment_id VARCHAR(100) REFERENCES receipts(payment_id),
                      ticket_issued BOOLEAN DEFAULT FALSE,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SLOTS TABLE
CREATE TABLE slots (
                       uuid text REFERENCES pass(uuid),
                       slot_no INT,
                       event_id INT REFERENCES events(id),
                       attended BOOLEAN DEFAULT FALSE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (uuid, slot_no)
);
