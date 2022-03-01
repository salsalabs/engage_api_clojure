drop table if exists history;
create table history (
    id integer not null primary key,
    item text,
    at timestamp default current_timestamp);
drop table if exists country;
create table country (
    id integer unique not null primary key,
    abbreviation varchar(2) not null unique,
    name text not null unique,
    at timestamp default current_timestamp);

insert into country(abbreviation, name) values("US", "United States of America");
insert into country(abbreviation, name) values("GB", "Great Britain");