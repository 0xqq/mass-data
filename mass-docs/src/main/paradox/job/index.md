# 调度（mass-job）

任务自动化高度是大数据时代数据整合后台必备的技术。在数据仓库、数据集市及各种数据池、湖中，任务调度让大量数据的进出、存放、清洗、过滤、
加工等工作有序、高效的展开。

## 架构

![任务调度架构](../static/SchedulerPlatformArchitecture.svg)

mass-job 架构上分三部分：应用层，核心控制层、目标层。

- **应用层**：提供命令行和Web图形界面客户端，用户可对任务进行配置、管理，启动、停止控制任务，监控系统运行状况。
- **核心控制层**：mass-scheduler的核心，任务调度、控制实现。分配任务到各Agent执行……
- **目标层**：mass-scheduler可适配的应用业务，ETL服务器、作业工作站等

## 调度类型

MassData提供完善的任务调度功能，支持简单调度（时间间隔）、日历调度和事件触发三种。同时，调度任务提供 `beforeStart`、`afterStart`、`beforeStart`和`afterStop`回调函数，
用户可据此实现对任务启动、退出时做更自定义操作，如：实现任务依赖等。

- 简单调度（时间间隔）：按指定的时间间隔调度任务，可配置任务将被执行次数（无限或N次）
- 日历调度：使用类似UNIX/Linux Crontab格式的策略进行基于日历时间的调度
- 事件触发：由某个事件触发调度执行。如：某个数据同步任务完成后发出事件通知分析系统对新数据进行分析。

任务配置（JobConfig.proto）属性：

@@snip [JobConfig](../../../../../mass-core/src/main/protobuf/mass/model/job/job.proto) { #JobConfig }

## 任务类型

- 代码任务（实现了SchedulerJob接口的任务，任务将和 MassData 在同一个进程或执行引擎集群上执行），代码任务可做为MassData平台的一个扩展。
- 应用程序任务（shell、jar等可执行程序，任务将在一个独立进程或执行引擎集群上执行）。通常的应用和业务都推荐使用应用程序任务进行提交。

## 应用程序任务提交方式

mass-scheduler提供两种任务提交方式：

1. REST接口上传zip包，应用可将任务打成zip包后提交到平台待执行。
0. 实现 `SchedulerJob` 接口，将代码打成jar包后放入 mass-job/lib 目录，再通过管理界面启用（扩展mass-scheduler功能）。

### Zip包

Zip包里面需要包含配置文件和可执行程序，配置文件使用 [HOCON](https://github.com/lightbend/config) 格式。每个配置文件代表一个Job。

@@snip [sample.conf](../../../../../mass-job/src/universal/examples/sample-job/sample.conf)

## 远程调度

MassData通过SSH支持任务调度支持远程启动程序，要使用此功能需要配置SSL免密码登录。

## 配置

### DDL (Postgres)

**调用任务DDL**

@@snip [ddl-pg](../../../../../scripts/software/postgres/init.sql) { #ddl-job }
