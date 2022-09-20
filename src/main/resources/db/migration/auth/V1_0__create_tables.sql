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

create table email_domain
(
    email_domain_id uuid not null
        constraint email_domain_pk
            primary key,
    name varchar(100) not null
        constraint email_domain_name_unique unique,
    description varchar(200)
);