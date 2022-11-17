ALTER TABLE user_group
    ADD CONSTRAINT pk_user_group PRIMARY KEY (user_id, group_id);

ALTER TABLE user_role
    ADD CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id);

ALTER TABLE group_assignable_role
    ADD CONSTRAINT pk_group_assignable_role PRIMARY KEY (group_id, role_id);
