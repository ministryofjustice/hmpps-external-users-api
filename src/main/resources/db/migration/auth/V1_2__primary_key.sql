ALTER TABLE user_group
    ADD CONSTRAINT pk_user_group PRIMARY KEY (user_id, group_id);

ALTER TABLE user_role
    ADD CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id);

