-- ==============================================
-- DROP TABLES (reverse order to handle FKs safely)
-- ==============================================
DROP TABLE IF EXISTS track CASCADE;
DROP TABLE IF EXISTS hack_reg_details CASCADE;
DROP TABLE IF EXISTS hack_passes CASCADE;
DROP TABLE IF EXISTS slots CASCADE;
DROP TABLE IF EXISTS passes CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS receipts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ==============================================
-- USERS TABLE
-- ==============================================
CREATE TABLE users (
                       email       TEXT PRIMARY KEY,
                       name        TEXT NOT NULL,
                       phone       TEXT,
                       created_at  TIMESTAMPTZ DEFAULT now()
);

-- ==============================================
-- RECEIPTS TABLE
-- ==============================================
CREATE TABLE receipts (
                          payment_id      VARCHAR(100) PRIMARY KEY,
                          paid_on         TIMESTAMP NOT NULL,
                          method          VARCHAR(50),
                          email           TEXT REFERENCES users(email),
                          phone           VARCHAR(20),
                          passGenerated   BOOLEAN DEFAULT FALSE,
                          amount          DECIMAL(10,2) NOT NULL
);

-- ==============================================
-- DEPARTMENTS TABLE
-- ==============================================
CREATE TABLE departments (
                             id      SERIAL PRIMARY KEY,
                             name    TEXT UNIQUE NOT NULL
);

-- ==============================================
-- EVENTS TABLE
-- ==============================================
CREATE TABLE events (
                        id              SERIAL PRIMARY KEY,
                        external_id     INT UNIQUE NOT NULL,
                        name            TEXT NOT NULL,
                        department_id   INT REFERENCES departments(id),
                        event_type      TEXT NOT NULL DEFAULT 'technical',
                        cost            DECIMAL(10,2) DEFAULT NULL,
                        registrations   INT DEFAULT 0,
                        created_at      TIMESTAMPTZ DEFAULT now()
);

-- ==============================================
-- PASSES TABLE
-- ==============================================
CREATE TABLE passes (
                        pass_id         TEXT PRIMARY KEY,
                        user_email      TEXT REFERENCES users(email),
                        payment_id      VARCHAR(100) REFERENCES receipts(payment_id),
                        ticket_issued   BOOLEAN DEFAULT FALSE,
                        created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================
-- SLOTS TABLE
-- ==============================================
CREATE TABLE slots (
                       pass_id     TEXT REFERENCES passes(pass_id),
                       slot_no     INT NOT NULL,
                       event_id    INT REFERENCES events(external_id),
                       attended    BOOLEAN DEFAULT FALSE,
                       created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (pass_id, slot_no)
);

-- ==============================================
-- HACK_PASSES / TEAM TICKETS (PARENT TABLE)
-- ==============================================
CREATE TABLE hack_passes (
                             team_id         TEXT PRIMARY KEY,                     -- Unique team identifier (text instead of UUID)
                             leader_email    TEXT NOT NULL,                        -- Leader’s email
                             team_name       VARCHAR(100) NOT NULL,                -- Team name
                             track           VARCHAR(100) NOT NULL,                -- Hackathon track
                             payment_id      VARCHAR(100) UNIQUE,                  -- Payment transaction/order ID
                             ticket_issued   BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             attended        BOOLEAN NOT NULL DEFAULT FALSE
);



-- ==============================================
-- HACK_REG_DETAILS (CHILD TABLE)
-- ==============================================
CREATE TABLE hack_reg_details (
                                  team_id         TEXT NOT NULL,                         -- Same as hack_passes.team_id
                                  email           TEXT NOT NULL,                         -- Participant email
                                  full_name       VARCHAR(150) NOT NULL,                 -- Participant name
                                  institution     VARCHAR(150),                          -- Institution
                                  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  phone_number    VARCHAR(20),
                                  gender          VARCHAR(10),
                                  department      VARCHAR(100),
                                  year_of_study   VARCHAR(10),
                                  PRIMARY KEY (team_id, email),
                                  CONSTRAINT hack_reg_details_team_fk FOREIGN KEY (team_id)
                                      REFERENCES hack_passes(team_id) ON DELETE CASCADE
);



-- ==============================================
-- TRACK TABLE
-- ==============================================
CREATE TABLE track (
                       team_id             TEXT PRIMARY KEY,
                       domain_name         VARCHAR(100) UNIQUE NOT NULL,
                       problem_statement   TEXT NOT NULL
);


-- ==============================================
-- INDEXES
-- ==============================================

-- Index on 'users' table
-- Reason: To speed up searching for users by their email.
CREATE INDEX idx_users_email ON users(email);

------------------------------------------------

-- Indexes on 'receipts' table
-- Reason: To efficiently join with the 'users' table on the foreign key 'email'.
CREATE INDEX idx_receipts_email ON receipts(email);

-- Reason: To speed up queries that filter receipts by date range (e.g., weekly reports).
CREATE INDEX idx_receipts_paid_on ON receipts(paid_on);

------------------------------------------------

-- Index on 'events' table
-- Reason: To efficiently join with the 'departments' table on the foreign key 'department_id'.
CREATE INDEX idx_events_department_id ON events(department_id);

-- Reason: To quickly filter events by their type (e.g., 'technical', 'workshop').
CREATE INDEX idx_events_event_type ON events(event_type);

------------------------------------------------

-- Indexes on 'passes' table
-- Reason: To efficiently join with the 'users' table and find passes for a specific user.
CREATE INDEX idx_passes_user_email ON passes(user_email);

-- Reason: To efficiently join with the 'receipts' table.
CREATE INDEX idx_passes_payment_id ON passes(payment_id);

------------------------------------------------

-- Index on 'slots' table
-- Reason: To quickly find all participants (passes) registered for a specific event.
CREATE INDEX idx_slots_event_id ON slots(event_id);
-- Note: An index on 'pass_id' is not needed as it is the leading column in the composite primary key.

------------------------------------------------

-- Indexes on 'hack_passes' table (Team Tickets)
-- Reason: To quickly find the team led by a specific user.
CREATE INDEX idx_hack_passes_leader_email ON hack_passes(leader_email);

-- Reason: To efficiently filter teams based on their hackathon track (e.g., 'AI/ML', 'Web3').
CREATE INDEX idx_hack_passes_track ON hack_passes(track);

------------------------------------------------

-- Index on 'hack_reg_details' table (Team Members)
-- Reason: To quickly find which team(s) a specific participant (by email) is a part of.
-- The primary key (team_id, email) is not efficient for queries filtering only on 'email'.
CREATE INDEX idx_hack_reg_details_email ON hack_reg_details(email);