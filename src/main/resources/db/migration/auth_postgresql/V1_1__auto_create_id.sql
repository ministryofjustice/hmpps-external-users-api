CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE groups ALTER group_id SET DEFAULT gen_random_uuid();
ALTER TABLE roles ALTER role_id SET DEFAULT gen_random_uuid();
ALTER TABLE email_domain ALTER email_domain_id SET DEFAULT gen_random_uuid();
ALTER TABLE child_group ALTER child_group_id SET DEFAULT gen_random_uuid();
