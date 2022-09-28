create table groups
(
    group_id uuid not null
        constraint group_pk
            primary key,
    group_code varchar(30) not null
        constraint group_code_uk
            unique,
    group_name varchar(100) not null,
    create_datetime timestamp default CURRENT_TIMESTAMP not null
);

create table child_group
(
    child_group_id uuid not null
        constraint child_group_pk
            primary key,
    child_group_code varchar(30) not null
        constraint child_group_code_uk
            unique,
    child_group_name varchar(100) not null,
    group_id uuid not null
        constraint child_group_group_id_fk
            references groups
);
create table roles
(
	role_id uuid not null
		constraint role_pk
			primary key,
	role_code varchar(50) not null
		constraint role_code_uk
			unique,
	role_name varchar(128),
	create_datetime timestamp default CURRENT_TIMESTAMP not null,
	role_description text,
	admin_type varchar(100) default 'EXT_ADM'::character varying not null
);

create table users
(
    user_id uuid not null
        constraint user_id_pk
            primary key,
    username varchar(240) not null
        constraint username_uk
            unique,
    password varchar(100),
    email varchar(240),
    first_name varchar(50),
    last_name varchar(50),
    verified boolean default false not null,
    locked boolean default false not null,
    enabled boolean default false not null,
    master boolean default false not null,
    create_datetime timestamp default CURRENT_TIMESTAMP not null,
    password_expiry timestamp default CURRENT_TIMESTAMP not null,
    last_logged_in timestamp default CURRENT_TIMESTAMP not null,
    source varchar(50) not null,
    mfa_preference varchar(15) default 'EMAIL'::character varying not null,
    inactive_reason varchar(100),
    pre_disable_warning boolean default false not null
);

create table group_assignable_role
(
    role_id uuid not null
        constraint group_assignable_role_role_fk
            references roles,
    group_id uuid not null
        constraint group_assignable_role_group_fk
            references groups,
    automatic boolean default false not null,
    create_datetime timestamp default CURRENT_TIMESTAMP not null
);


create index group_assignable_role_role_fk
    on group_assignable_role (role_id);

create index group_assignable_role_group_fk
    on group_assignable_role (group_id);

create table user_role
(
    role_id uuid not null
        constraint user_role_role_id_fk
            references roles,
    user_id uuid not null
        constraint user_role_user_id_fk
            references users,
    create_datetime timestamp default CURRENT_TIMESTAMP not null
);


create index user_role_user_id_fk
    on user_role (user_id);

create index user_role_role_id_fk
    on user_role (role_id);


create table user_group
(
    group_id uuid not null
        constraint user_group_group_id_fk
            references groups,
    user_id uuid not null
        constraint user_group_user_id_fk
            references users,
    create_datetime timestamp default CURRENT_TIMESTAMP not null
);


create index user_group_user_id_fk
    on user_group (user_id);

create index user_group_group_id_fk
    on user_group (group_id);

create table email_domain
(
    email_domain_id uuid not null
        constraint email_domain_pk
            primary key,
    name varchar(100) not null
        constraint email_domain_name_unique unique,
    description varchar(200)
);