# 第三方依赖与许可声明（THIRD-PARTY NOTICES）

本工程源码为独立编写。运行期引入以下第三方开源依赖，均以宽松许可发布：

## 运行期依赖

| 依赖 | 版本 | 许可证 |
|------|------|--------|
| Spring Boot（starter-web / validation / aop / jdbc / actuator 等） | 3.2.5 | Apache-2.0 |
| Spring Framework / Spring Data / Spring Security Crypto | 6.1.x | Apache-2.0 |
| MyBatis-Plus（starter / core / extension / annotation） | 3.5.5 | Apache-2.0 |
| MyBatis / mybatis-spring | 3.5.15 / 3.0.3 | Apache-2.0 |
| JSqlParser | 4.6 | Apache-2.0 |
| Sa-Token（spring-boot3-starter / core / redis-jackson / jakarta-servlet） | 1.37.0 | Apache-2.0 |
| AWS SDK for Java v2（s3 及协议模块） | 2.25.60 | Apache-2.0 |
| HikariCP | 5.0.1 | Apache-2.0 |
| Lettuce / Netty / Reactor / Reactive Streams | 6.3.x / 4.1.x / 3.6.x | Apache-2.0 |
| Micrometer / Prometheus simpleclient | 1.12.x / 0.16.0 | Apache-2.0 |
| Jackson（databind / core / datatype / module） | 2.15.4 | Apache-2.0 |
| SnakeYAML | 2.2 | Apache-2.0 |
| Hibernate Validator / Jakarta Validation | 8.0.1 / 3.0.2 | Apache-2.0 |
| Tomcat Embed（core / el / websocket） | 10.1.20 | Apache-2.0 |
| Apache Log4j（api / to-slf4j） | 2.21.1 | Apache-2.0 |
| AspectJ Weaver | 1.9.22 | EPL-1.0 |
| Logback（classic / core） | 1.4.14 | EPL-1.0 / LGPL-2.1（双许可） |
| Lombok | 1.18.32 | MIT |

## 测试期依赖

| 依赖 | 许可证 |
|------|--------|
| JUnit 5 / Spring Boot Test | EPL-2.0 / Apache-2.0 |
| H2 Database | EPL-1.0 / MPL-2.0（双许可） |

## 数据库驱动（部署时自备，不入库）

| 依赖 | 许可证 |
|------|--------|
| MySQL Connector/J | GPL-2.0-with-classpath-exception |

## 声明

- 上述依赖以 Maven 坐标从公开仓库获取，本仓库不打包、不再分发任何第三方构件的副本。
- 各依赖的完整许可文本以其官方仓库为准。
- 本工程不含 GPL 类传染性许可的源码混入；唯一 GPL 相关项为部署时自行下载的 MySQL Connector/J 驱动，其 FOSS 例外条款允许与 Apache-2.0 工程配合使用。
