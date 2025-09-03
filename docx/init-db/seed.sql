-- Clear existing data (truncate in dependency order)
TRUNCATE TABLE slots CASCADE;
TRUNCATE TABLE passes CASCADE;
TRUNCATE TABLE receipts CASCADE;
TRUNCATE TABLE events CASCADE;
TRUNCATE TABLE departments CASCADE;
TRUNCATE TABLE users CASCADE;


-- Seed Events
-- Seed Departments
INSERT INTO departments (name) VALUES
                                   ('MECH'),
                                   ('CSE'),
                                   ('EEE'),
                                   ('COM'),
                                   ('CIVIL');

-- Seed Events
INSERT INTO events (external_id, name, department_id) VALUES
                                                          (10, 'Aero Velocity Drone Racing', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (11, 'Murder Mystery', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (9, 'Bottle Rocketrix', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (12, 'Squid Games', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (3, 'Hovercraft', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (2, 'Ready Player One', (SELECT id FROM departments WHERE name = 'CSE')),
                                                          (4, 'Blitz GP', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (5, 'Mechathlon', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (6, 'Robo Wars', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (7, 'Paper Presentation', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (8, 'Project Presentation', (SELECT id FROM departments WHERE name = 'MECH')),
                                                          (17, 'Power House', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (19, 'BizQuiz', (SELECT id FROM departments WHERE name = 'COM')),
                                                          (24, 'IPL Auction', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (20, 'Electrothon', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (16, 'F1 Pit Escape - Save the Race!', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (18, 'Flip The Bit', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (14, 'Circuit Clash', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (21, 'Endgame', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (22, 'Ideate - Paper Presentation', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (23, 'Tentukotta', (SELECT id FROM departments WHERE name = 'EEE')),
                                                          (25, 'Startup Showdown', (SELECT id FROM departments WHERE name = 'COM')),
                                                          (27, 'BOB THE BUILDER', (SELECT id FROM departments WHERE name = 'CIVIL')),
                                                          (29, 'REAL ESTATE', (SELECT id FROM departments WHERE name = 'CIVIL')),
                                                          (37, 'Finema', (SELECT id FROM departments WHERE name = 'COM'));
