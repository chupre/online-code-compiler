create table executions
(
    id                bigserial   not null
        constraint executions_pk
            primary key,
    code              text        not null,
    language          varchar(20) not null,
    output            text,
    status            varchar(20) not null,
    execution_time_ms int         not null,
    created_at        timestamptz
);

