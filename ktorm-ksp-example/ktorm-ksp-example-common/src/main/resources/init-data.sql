create table "t_department"
(
    "id"        int          not null primary key auto_increment,
    "name"      varchar(128) not null,
    "location"  varchar(128) not null,
    "mixedCase" varchar(128)
);

create table "t_employee"
(
    "id"            int          not null primary key auto_increment,
    "name"          varchar(128) not null,
    "job"           varchar(128) not null,
    "manager_id"    int          null,
    "gender"        int          null,
    "hire_date"     date         not null,
    "salary"        bigint       not null,
    "department_id" int          not null
);

create schema "company";
create table "company"."t_customer"
(
    "id"           int          not null primary key auto_increment,
    "name"         varchar(128) not null,
    "email"        varchar(128) not null,
    "phone_number" varchar(128) not null
);

insert into "t_department"("name", "location")
values ('tech', 'Guangzhou');
insert into "t_department"("name", "location")
values ('finance', 'Beijing');

insert into "t_employee"("name", "job", "manager_id", "gender", "hire_date", "salary", "department_id")
values ('vince', 'engineer', null, 0, '2018-01-01', 100, 1);
insert into "t_employee"("name", "job", "manager_id", "gender", "hire_date", "salary", "department_id")
values ('marry', 'trainee', 1, 0, '2019-01-01', 50, 1);

insert into "t_employee"("name", "job", "manager_id", "gender", "hire_date", "salary", "department_id")
values ('tom', 'director', null, 1, '2018-01-01', 200, 2);
insert into "t_employee"("name", "job", "manager_id", "gender", "hire_date", "salary", "department_id")
values ('penny', 'assistant', 3, 1, '2019-01-01', 100, 2);

insert into "company"."t_customer"("name", "email", "phone_number")
values ('lucy', 'lucy@email.com', '12311111111')

