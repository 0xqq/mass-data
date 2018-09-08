set timezone to 'Asia/Chongqing';
select now();
create user massdata with nosuperuser
  replication
  encrypted password 'Massdata.2018';
create database massdata owner = massdata template = template0 encoding = 'UTF-8' lc_ctype = 'zh_CN.UTF-8' lc_collate = 'zh_CN.UTF-8';
\c massdata;
create extension adminpack;
create extension hstore;

----------------------------------------
-- #functions
----------------------------------------

-- 将数组反序
create or replace function array_reverse(anyarray)
  returns anyarray as $$
select array(
         select $1 [ i ] from generate_subscripts($1, 1) as s (i) order be i desc
           );
$$
language 'sql'
strict
immutable;
----------------------------------------
-- #functions
----------------------------------------

----------------------------------------
-- init tables, views, sequences  begin
----------------------------------------

-- #ddl-job
drop table if exists public.job_item;
create table public.job_item (
  key        varchar(128) primary key,
  config     jsonb       not null,
  creator    varchar(36) not null,
  created_at timestamptz not null
);

drop table if exists public.job_trigger;
create table public.job_trigger (
  key        varchar(128) not null primary key,
  config     jsonb       not null,
  creator    varchar(36) not null,
  created_at timestamptz not null
);

drop table if exists public.job_schedule;
create table public.job_schedule (
  job_key     varchar(128) not null,
  trigger_key varchar(128) not null,
  description text         not null,
  status      int          not null default 1,
  created_at  timestamptz  not null,
  constraint job_schedule_pk primary key (job_key, trigger_key)
);
comment on table public.job_schedule
is '调度任务，job_item与job_trigger关联后实际执行';
comment on column public.job_schedule.status
is '调度任务闫：0 未启用，1 启用';

drop table if exists public.job_log;
create table public.job_log (
  id                char(24) primary key,
  job_key           varchar(128), -- FK job_item.key
  trigger_key       varchar(128), -- FK job_trigger.key
  start_time        timestamptz not null,
  completion_time   timestamptz,
  completion_status int,
  completion_value  text
);
-- #ddl-job

-- #ddl-workflow
drop table if exists public.wf_detail;
create table public.wf_detail (
  name       varchar(128) not null primary key,
  content    text         not null,
  created_at timestamptz
);
comment on column public.wf_detail.content
is '工作流配置（XML）';
comment on column public.wf_detail.created_at
is '创建时间';
-- #ddl-workflow

----------------------------------------
-- init tables, views, sequences  end
----------------------------------------

-- change tables, views, sequences owner to massdata
DO $$DECLARE r record;
BEGIN
  FOR r IN SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'
  LOOP
    EXECUTE 'alter table ' || r.table_name || ' owner to massdata;';
  END LOOP;
END$$;
DO $$DECLARE r record;
BEGIN
  FOR r IN select sequence_name from information_schema.sequences where sequence_schema = 'public'
  LOOP
    EXECUTE 'alter sequence ' || r.sequence_name || ' owner to massdata;';
  END LOOP;
END$$;
DO $$DECLARE r record;
BEGIN
  FOR r IN select table_name from information_schema.views where table_schema = 'public'
  LOOP
    EXECUTE 'alter table ' || r.table_name || ' owner to massdata;';
  END LOOP;
END$$;
-- grant all privileges on all tables in schema public to massdata;
-- grant all privileges on all sequences in schema public to massdata;
