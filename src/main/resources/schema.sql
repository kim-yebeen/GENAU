-- 1. 기존 테이블 삭제 (존재 시)
DROP TABLE IF EXISTS todolist_file;
DROP TABLE IF EXISTS todolist_assignees;
DROP TABLE IF EXISTS todolist;
DROP TABLE IF EXISTS users;

-- 2. users 테이블
CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       user_name VARCHAR(255) NOT NULL,
                       mail VARCHAR(255) UNIQUE NOT NULL,
                       user_pw VARCHAR(255) NOT NULL,
                       user_img VARCHAR(512)
);

-- 3. todolist 테이블 (creator_id는 users 참조)
CREATE TABLE todolist (
                          todo_id BIGSERIAL PRIMARY KEY,
                          team_id BIGINT NOT NULL,
                          cat_id BIGINT,
                          todo_title VARCHAR(255),
                          todo_des TEXT,
                          due_date DATE,
                          todo_time TIMESTAMP,
                          file_form VARCHAR(20),
                          todo_checked BOOLEAN DEFAULT FALSE,
                          creator_id BIGINT,
                          convert_status VARCHAR(20),
                          converted_file_url TEXT,
                          converted_at TIMESTAMP,
                          uploaded_file_path TEXT,

                          CONSTRAINT fk_creator
                              FOREIGN KEY (creator_id)
                                  REFERENCES users(user_id)
                                  ON DELETE SET NULL
);

-- 4. N:M 관계 - 담당자(todolist_assignees)
CREATE TABLE todolist_assignees (
                                    todo_id BIGINT,
                                    user_id BIGINT,
                                    PRIMARY KEY (todo_id, user_id),
                                    FOREIGN KEY (todo_id) REFERENCES todolist(todo_id) ON DELETE CASCADE,
                                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 5. 1:N 관계 - 파일들(todolist_file)
CREATE TABLE todolist_file (
                               file_id BIGSERIAL PRIMARY KEY,
                               todo_id BIGINT NOT NULL,
                               file_name VARCHAR(255),
                               file_path TEXT,
                               content_type VARCHAR(100),
                               uploaded_at TIMESTAMP,
                               uploader_id BIGINT,

                               FOREIGN KEY (todo_id) REFERENCES todolist(todo_id) ON DELETE CASCADE,
                               FOREIGN KEY (uploader_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS users (
                                     user_id     SERIAL PRIMARY KEY,
                                     user_name   VARCHAR(255) NOT NULL,
    user_pw     VARCHAR(255) NOT NULL,
    mail        VARCHAR(255) UNIQUE NOT NULL,
    user_image  TEXT
    );

CREATE TABLE IF NOT EXISTS team (
                                    team_id     SERIAL PRIMARY KEY,
                                    team_name   VARCHAR(255) NOT NULL,
    team_des    TEXT,               -- 여기 쉼표 추가
    team_image  TEXT
    );

CREATE TABLE IF NOT EXISTS teammates (
                                         teammates_id      SERIAL PRIMARY KEY,
                                         user_id           INT REFERENCES users(user_id)   ON DELETE CASCADE,
    team_id           INT REFERENCES team(team_id)     ON DELETE CASCADE,
    team_participated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 쉼표 추가
    team_manage       BOOLEAN
    );

CREATE TABLE IF NOT EXISTS category (
                                        cat_id     SERIAL PRIMARY KEY,
                                        team_id    INT REFERENCES team(team_id) ON DELETE CASCADE,
    cat_name   VARCHAR(255) NOT NULL
    );

CREATE TABLE IF NOT EXISTS todolist (
                                        todo_id      SERIAL PRIMARY KEY,
                                        cat_id       INT REFERENCES category(cat_id) ON DELETE SET NULL,
    team_id      INT REFERENCES team(team_id)     ON DELETE CASCADE,
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

CREATE TABLE IF NOT EXISTS notice (
    notice_id      SERIAL PRIMARY KEY,
    teammates_id   INT REFERENCES teammates(teammates_id) ON DELETE CASCADE,
    notice_type    VARCHAR(50) NOT NULL,
    reference_id   INT,
    notice_message TEXT,
    notice_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read        BOOLEAN DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS invitation (
                                          invitation_id   SERIAL PRIMARY KEY,
                                          team_id         INT         NOT NULL
                                          REFERENCES team(team_id)
    ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,        -- 초대할 유저 이메일
    token           VARCHAR(64)  NOT NULL UNIQUE, -- 수락용 랜덤 토큰(UUID)
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP    NOT NULL,        -- 토큰 만료 시각
    accepted        BOOLEAN      NOT NULL DEFAULT FALSE
);

