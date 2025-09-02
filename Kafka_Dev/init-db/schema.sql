-- DROP TABLES (reverse order to handle FKs safely)
DROP TABLE IF EXISTS slots CASCADE;
DROP TABLE IF EXISTS passes CASCADE;
DROP TABLE IF EXISTS receipts CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS users CASCADE;


-- USERS TABLE
CREATE TABLE users (
                       email TEXT PRIMARY KEY,
                       name TEXT NOT NULL,
                       phone TEXT,
                       created_at TIMESTAMPTZ DEFAULT now()

);

-- RECEIPTS TABLE
CREATE TABLE receipts (
                          payment_id VARCHAR(100) PRIMARY KEY,
                          paid_on TIMESTAMP NOT NULL,
                          method VARCHAR(50),
                          email TEXT REFERENCES users(email),
                          phone VARCHAR(20),
                          passGenerated BOOLEAN DEFAULT FALSE,
                          amount DECIMAL(10,2) NOT NULL

);

CREATE TABLE departments (
                             id SERIAL PRIMARY KEY,
                             name TEXT UNIQUE NOT NULL
);

-- EVENTS TABLE
CREATE TABLE events (
                        id SERIAL PRIMARY KEY,
                        external_id INT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        department_id INT REFERENCES departments(id),
                        event_type TEXT NOT NULL DEFAULT 'technical',
                        registrations INT DEFAULT 0,
                        created_at TIMESTAMPTZ DEFAULT now()
);


-- PASS TABLE
CREATE TABLE passes (
                      pass_id text PRIMARY KEY,
                      user_email TEXT REFERENCES users(email),
                      payment_id VARCHAR(100) REFERENCES receipts(payment_id),
                      ticket_issued BOOLEAN DEFAULT FALSE,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SLOTS TABLE
CREATE TABLE slots (
                       pass_id text REFERENCES passes(pass_id),
                       slot_no INT NOT NULL,
                       event_id INT REFERENCES events(external_id),
                       attended BOOLEAN DEFAULT FALSE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (pass_id, slot_no)
);

DROP TABLE IF EXISTS hack_reg_details CASCADE;
DROP TABLE IF EXISTS hack_passes CASCADE;
-- ==============================================
-- HACK_PASSES / TEAM TICKETS (PARENT TABLE)
-- ==============================================
CREATE TABLE hack_passes (
                             team_id         TEXT PRIMARY KEY,              -- Unique team identifier (text instead of UUID)
                             leader_email    TEXT NOT NULL,               -- Leader’s email
                             team_name       VARCHAR(100) NOT NULL,         -- Team name
                             track           VARCHAR(100) NOT NULL,         -- Hackathon track
                             payment_id      VARCHAR(100) UNIQUE,           -- Payment transaction/order ID
                             ticket_issued   BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             attended        BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes for performance
CREATE INDEX idx_hack_passes_leader_email ON hack_passes(leader_email);
CREATE INDEX idx_hack_passes_payment_id ON hack_passes(payment_id);


-- ==============================================
-- HACK_REG_DETAILS (CHILD TABLE)
-- ==============================================
CREATE TABLE hack_reg_details (
                                  team_id         TEXT NOT NULL,                 -- Same as hack_passes.team_id
                                  email           TEXT NOT NULL,               -- Participant email
                                  full_name       VARCHAR(150) NOT NULL,         -- Participant name
                                  institution     VARCHAR(150),                  --  institution
                                  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  phone_number    VARCHAR(20),

                                  PRIMARY KEY (team_id, email),
                                  CONSTRAINT hack_reg_details_team_fk FOREIGN KEY (team_id) REFERENCES hack_passes(team_id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_hack_reg_details_email ON hack_reg_details(email);
CREATE INDEX idx_hack_reg_details_institution ON hack_reg_details(institution);

