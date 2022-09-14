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
