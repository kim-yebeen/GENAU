
-- 통합된 users 테이블
CREATE TABLE IF NOT EXISTS users (
                       user_id BIGSERIAL PRIMARY KEY,
                       user_name VARCHAR(255) NOT NULL,
                       mail VARCHAR(255) UNIQUE NOT NULL,
                       user_pw VARCHAR(255) NOT NULL,
                       user_image TEXT  -- user_img와 user_image 통합
);

-- team 테이블
CREATE TABLE IF NOT EXISTS team (
                      team_id BIGSERIAL PRIMARY KEY,
                      team_name VARCHAR(255) NOT NULL,
                      team_des TEXT,
                      team_image TEXT
);

-- teammates 테이블
CREATE TABLE IF NOT EXISTS teammates (
                           teammates_id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                           team_id BIGINT REFERENCES team(team_id) ON DELETE CASCADE,
                           team_participated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           team_manage BOOLEAN DEFAULT FALSE
);

-- category 테이블
CREATE TABLE IF NOT EXISTS category (
                          cat_id BIGSERIAL PRIMARY KEY,
                          team_id BIGINT REFERENCES team(team_id) ON DELETE CASCADE,
                          cat_name VARCHAR(255) NOT NULL
);

-- 통합된 todolist 테이블
CREATE TABLE IF NOT EXISTS todolist (
                          todo_id BIGSERIAL PRIMARY KEY,
                          team_id BIGINT REFERENCES team(team_id) ON DELETE CASCADE,
                          cat_id BIGINT REFERENCES category(cat_id) ON DELETE SET NULL,
                          todo_title VARCHAR(255) NOT NULL,
                          todo_des TEXT,
                          due_date DATE,
                          todo_time TIMESTAMP,
                          file_form VARCHAR(20),
                          todo_checked BOOLEAN DEFAULT FALSE,
                          creator_id BIGINT REFERENCES users(user_id) ON DELETE SET NULL,
                          convert_status VARCHAR(20),
                          converted_file_url TEXT,
                          converted_at TIMESTAMP,
                          uploaded_file_path TEXT
);

-- todolist_assignees (N:M 관계)
CREATE TABLE IF NOT EXISTS todolist_assignees (
                                    todo_id BIGINT REFERENCES todolist(todo_id) ON DELETE CASCADE,
                                    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                                    PRIMARY KEY (todo_id, user_id)
);

-- todolist_file (1:N 관계)
CREATE TABLE IF NOT EXISTS todolist_file (
                               file_id BIGSERIAL PRIMARY KEY,
                               todo_id BIGINT NOT NULL REFERENCES todolist(todo_id) ON DELETE CASCADE,
                               file_name VARCHAR(255),
                               file_path TEXT,
                               content_type VARCHAR(100),
                               uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               uploader_id BIGINT REFERENCES users(user_id) ON DELETE SET NULL
);

-- notice 테이블
CREATE TABLE IF NOT EXISTS notice (
                        notice_id BIGSERIAL PRIMARY KEY,
                        teammates_id BIGINT REFERENCES teammates(teammates_id) ON DELETE CASCADE,
                        notice_type VARCHAR(50) NOT NULL,
                        reference_id BIGINT,
                        notice_message TEXT,
                        notice_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        is_read BOOLEAN DEFAULT FALSE
);

-- invitation 테이블
CREATE TABLE IF NOT EXISTS invitation (
                            invitation_id BIGSERIAL PRIMARY KEY,
                            team_id BIGINT NOT NULL REFERENCES team(team_id) ON DELETE CASCADE,
                            email VARCHAR(255) NOT NULL,
                            token VARCHAR(64) NOT NULL UNIQUE,
                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            expires_at TIMESTAMP NOT NULL,
                            accepted BOOLEAN NOT NULL DEFAULT FALSE
);
