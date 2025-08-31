-- Clear existing data (truncate in dependency order)
TRUNCATE TABLE slots CASCADE;
TRUNCATE TABLE pass CASCADE;
TRUNCATE TABLE receipts CASCADE;
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE events CASCADE;

-- Seed Events
INSERT INTO events (name) VALUES
                              ('Tech Conference 2025'),
                              ('Music Festival 2025'),
                              ('Startup Pitch Day');
