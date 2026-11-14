DROP TABLE IF EXISTS user_action, event_similarity CASCADE;

CREATE TABLE IF NOT EXISTS user_action (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY(user_id, event_id)
);

CREATE TABLE IF NOT EXISTS event_similarity (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score FLOAT NOT NULL,
    calc_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY(event_a, event_b)
);