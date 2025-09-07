alter table executions
    alter column execution_time_ms drop not null;

alter table executions
    alter column created_at set not null;

alter table executions
    alter column created_at set default now();

alter table executions
    add executed_at timestamptz;