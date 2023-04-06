CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE users ALTER user_id SET DEFAULT gen_random_uuid();
