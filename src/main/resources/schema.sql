CREATE TABLE IF NOT EXISTS "user" (
    user_id     SERIAL PRIMARY KEY,
    user_name   VARCHAR(255) NOT NULL,
    user_pw     VARCHAR(255) NOT NULL,
    mail        VARCHAR(255) UNIQUE NOT NULL
    );

CREATE TABLE IF NOT EXISTS team (
    team_id     SERIAL PRIMARY KEY,
    team_name   VARCHAR(255) NOT NULL,
    team_des    TEXT
    );

CREATE TABLE IF NOT EXISTS teammates (
    teammates_id       SERIAL PRIMARY KEY,
    user_id            INT REFERENCES "user"(user_id) ON DELETE CASCADE,
    team_id            INT REFERENCES team(team_id) ON DELETE CASCADE,
    team_participated  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS category (
    cat_id     SERIAL PRIMARY KEY,
    team_id    INT REFERENCES team(team_id) ON DELETE CASCADE,
    cat_name   VARCHAR(255) NOT NULL
    );

CREATE TABLE IF NOT EXISTS todolist (
    todo_id      SERIAL PRIMARY KEY,
    cat_id       INT REFERENCES category(cat_id) ON DELETE SET NULL,
    team_id      INT REFERENCES team(team_id) ON DELETE CASCADE,
    teammates_id INT REFERENCES teammates(teammates_id) ON DELETE SET NULL,
    todo_title   VARCHAR(255) NOT NULL,
    todo_des     TEXT,
    todo_checked BOOLEAN DEFAULT FALSE,
    due_date     DATE,
    todo_time    TIMESTAMP,
    file_id      INT,
    file_form    VARCHAR(50),
    file_name    VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS notice
(
    notice_id      SERIAL PRIMARY KEY,
    teammates_id   INT REFERENCES teammates(teammates_id) ON DELETE CASCADE,
    notice_type    VARCHAR(50) NOT NULL,
    reference_id   INT,
    notice_message TEXT,
    notice_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read        BOOLEAN DEFAULT FALSE
    );
