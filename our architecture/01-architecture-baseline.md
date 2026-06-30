# ARCHITECTURE BASELINE CONVENTION - NỀN KIẾN TRÚC MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.0  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Java 21, Spring Boot 3.5.x, PostgreSQL 17, Flyway, Spring Data JPA/Hibernate  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\MAR_ARCHITECTURE_VERSION_CONVENTION.md` - Baseline MAR-ARCH-1.0
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\README.md` - Index OASIS architecture
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\coding_convention.md` - Pattern viết convention
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\database_convention.md` - Pattern DB/Flyway tham khảo
- `D:\Documents-for-Expert-Design-Database\MAR\docs BA\README.md` - Trạng thái BA docs v2.1

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chốt nền kiến trúc kỹ thuật cho dự án **MAR Lead-to-Enrollment MVP**.

Mục đích:

- Đóng điểm mở `SP1-D01` ở mức version line và technology baseline.
- Cho phép backend team bootstrap project đúng ngay từ đầu.
- Ngăn việc bê nguyên OASIS stack/domain/package vào MAR.
- Làm nền cho các convention chi tiết: API, DB, security, audit, testing, release.

## 2. PHẠM VI ÁP DỤNG

- **Áp dụng cho:** backend MAR, Sprint 1 foundation, các module tenant/branch/user/permission/catalog/import/audit.
- **Áp dụng cho:** project bootstrap, dependency baseline, package root, DB/migration baseline.
- **Không áp dụng cho:** frontend framework, production deployment topology, external identity provider final, reporting/dashboard R1C.
- **Stack:** Java 21, Spring Boot 3.5.x, PostgreSQL 17, Flyway, Spring Data JPA/Hibernate, Spring Security 6.x.
- **Spring Boot patch target:** default target `3.5.14` nếu artifact/BOM khả dụng và được Tech Lead xác nhận trong bước bootstrap; nếu không, dùng patch mới nhất thuộc line `3.5.x` đã được duyệt.

## 3. NGUYÊN TẮC CHUNG

> **"MAR reuse reference discipline, not reference-domain or stack assumptions."**

1. Backend MAR là REST API-first, không render UI trong server.
2. PostgreSQL là source of truth; Flyway là source of truth cho schema change.
3. Tenant isolation và permission correctness quan trọng hơn tốc độ làm UI.
4. Chọn dependency ổn định, dễ tuyển người, dễ vận hành trước khi chọn công nghệ mới.
5. Modular monolith trước, microservice/event bus sau khi có lý do thật.
6. Convention này thắng OASIS reference khi có mâu thuẫn với bối cảnh MAR.
7. Development commitment vẫn phụ thuộc Sprint sign-off, không chỉ phụ thuộc tài liệu này.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Tên project và artifact

| Loại | Convention | Ví dụ |
|---|---|---|
| Maven groupId | `vn.mar` | `vn.mar` |
| Maven artifactId | lowercase-kebab | `mar-api` |
| Root package | lowercase dot-separated | `vn.mar` |
| Main class | PascalCase | `MarApplication` |
| Application name | lowercase-kebab | `mar-api` |
| Profile | lowercase | `local`, `qa`, `prod` |

### 4.2. Tên module cấp cao

| Domain | Package |
|---|---|
| Common utilities | `vn.mar.common` |
| Security mechanics | `vn.mar.security` |
| Authentication | `vn.mar.auth` |
| Authorization | `vn.mar.authz` |
| Audit | `vn.mar.audit` |
| Tenant | `vn.mar.tenant` |
| Branch | `vn.mar.branch` |
| User | `vn.mar.user` |
| Role | `vn.mar.role` |
| Permission | `vn.mar.permission` |
| Catalog | `vn.mar.catalog` |
| Lead import | `vn.mar.leadimport` |

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Cấu trúc package chuẩn

```text
vn.mar
├── MarApplication.java
├── common
│   ├── config
│   ├── dto
│   ├── error
│   ├── exception
│   ├── logging
│   ├── mapper
│   ├── pagination
│   ├── search
│   ├── tenant
│   ├── time
│   └── util
├── security
│   ├── config
│   ├── context
│   ├── filter
│   └── jwt
├── auth
├── authz
├── audit
├── tenant
├── branch
├── user
├── role
├── permission
├── catalog
├── leadimport
├── notification
└── reporting
```

**Quy tắc đồng bộ package:**

- `vn.mar.role` được giữ trong baseline vì REST convention có `/api/v1/roles`; nếu Sprint 1 chỉ seed enum mà chưa có role API, package này là reserved và không thêm business code nếu không có ticket.
- `common.tenant` chứa tenant context/helper dùng chung, không chứa business rule của module `tenant`.
- `common.time` chứa `TimeProvider`/`Clock` abstraction để test audit/import/SLA/timezone dễ hơn.
- `notification` và `reporting` là reserved packages trong Sprint 1; không implement business code ở đây nếu ticket không yêu cầu rõ.

### 5.2. Cấu trúc project

```text
mar-api
├── pom.xml
├── mvnw
├── mvnw.cmd
├── src
│   ├── main
│   │   ├── java/vn/mar
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-qa.yml
│   │       ├── application-prod.yml
│   │       └── db/migration
│   └── test
│       ├── java/vn/mar
│       └── resources
└── README.md
```

### 5.3. Quy tắc một file / một class

- Mỗi file Java chỉ có một top-level public class/record/interface.
- Tên file trùng tên class/record/interface.
- Không tạo package `view`, `page`, `template`, `serverrender` trong backend.

### 5.4. Local/QA environment baseline

Local/QA baseline bắt buộc đủ để dev và QA dựng lại môi trường sạch:

```text
local profile: application-local.yml
qa profile: application-qa.yml
database: PostgreSQL 17
migration: Flyway from empty database
test DB: Testcontainers PostgreSQL for integration tests
seed: system/demo/test seed runner, separated from production migration
```

Khuyến nghị local orchestration:

```text
Docker Compose:
- PostgreSQL 17
- mar-api app when needed
- seed runner for local/QA fixtures
```

Rules:

- Production không tự load demo/test seed.
- Tenant A/B, Admin/Advisor fixtures dùng cho QA phải tạo được lặp lại.
- Fresh DB migration là điều kiện trước Sprint 1 release gate.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Maven baseline pattern

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <!-- Default target; verify availability during project bootstrap. -->
    <version>3.5.14</version>
    <relativePath/>
</parent>

<groupId>vn.mar</groupId>
<artifactId>mar-api</artifactId>
<version>0.1.0-SNAPSHOT</version>

<properties>
    <java.version>21</java.version>
</properties>
```

**Quy tắc:**

- BẮT BUỘC dùng Java 21.
- BẮT BUỘC dùng Maven Wrapper.
- BẮT BUỘC ở Spring Boot line `3.5.x`; patch version do Tech Lead verify trong bootstrap.
- Nếu `3.5.14` không có trong artifact repository được duyệt, dùng patch mới nhất thuộc `3.5.x` đã được approve.
- KHÔNG tự ý nâng Spring Boot 4.x trong Sprint 1.
- KHÔNG dùng Java 23 cho MAR MVP baseline.

### 6.2. Required dependency pattern

Minimum dependencies:

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-security
spring-boot-starter-data-jpa
spring-boot-starter-actuator
postgresql
flyway-core
spring-boot-starter-test
spring-security-test
testcontainers-postgresql
```

Optional with Tech Lead approval:

```text
lombok
mapstruct
jjwt-api / jjwt-impl / jjwt-jackson
caffeine
```

### 6.3. Application configuration pattern

```yaml
spring:
  application:
    name: mar-api
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
    baseline-on-migrate: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

**Quy tắc:**

- `ddl-auto=validate` là bắt buộc.
- Flyway quản lý schema.
- Secret/config nhạy cảm lấy từ environment/secret manager.

### 6.4. Modular monolith pattern

MAR Sprint 1 dùng modular monolith:

```text
one backend application
clear package/module boundaries
shared DB
domain services separated by package
```

Không split microservice cho Sprint 1.

## 7. QUY TẮC RIÊNG CỦA MAR BASELINE

### 7.1. OASIS reference mapping

Reuse:

- Cách viết convention.
- Discipline về package, review, test.
- Pattern Spring Security, audit, logging, Flyway, JPA.

Không reuse:

- Domain cũ như inspection/site/signup.
- OASIS package names.
- OASIS DB syntax.
- Server-side UI convention.

### 7.2. Backend/frontend boundary

Backend chỉ cung cấp:

- REST JSON API.
- Authentication/authorization context.
- File/import APIs khi scope tới.
- Health/metrics endpoints.

Backend không cung cấp:

- Server-rendered HTML.
- UI template.
- Frontend routing.
- CSS/JS asset pipeline.

### 7.3. Version control of decisions

Nếu muốn đổi một dòng trong baseline này, phải tạo decision mới:

- Vì sao đổi?
- Tác động đến docs BA?
- Tác động đến code?
- Migration/compatibility risk?
- Ai approve?

### 7.4. Minimal audit baseline

Minimal AuditLog/AuditEvent là bắt buộc cho Sprint 1 với các thay đổi nhạy cảm:

- Permission matrix changed.
- Role permission changed.
- User created/status changed/branch assigned.
- Tenant status changed.
- Branch status changed.
- Security permission denied for sensitive endpoints.
- Import batch created nếu Sprint 1 có API/fixture tạo batch.

Audit chi tiết theo `08-audit-convention.md`, nhưng baseline này khóa nguyên tắc: sensitive setup change không được merge nếu thiếu audit hoặc thiếu quyết định miễn audit rõ ràng.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - Main class đúng root package

```java
package vn.mar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarApplication.class, args);
    }
}
```

**Tại sao tốt:**

- Root package `vn.mar` scan được toàn bộ module con.
- Không phụ thuộc tên project thử nghiệm.
- Không lẫn package OASIS.

### 8.2. Bad example - Package và version sai baseline

```java
package com.example.demo;

@SpringBootApplication
public class DemoApplication {
}
```

```xml
<java.version>23</java.version>
<version>4.0.5</version>
```

**Tại sao sai:**

- Package không thể hiện domain MAR.
- Java 23 không phải LTS baseline đã chốt.
- Spring Boot 4.x tăng rủi ro dependency cho Sprint 1.

## 9. ANTI-PATTERNS CẦN TRÁNH

| Không làm | Làm đúng |
|---|---|
| Bootstrap project bằng package tạm `com.example` | Dùng `vn.mar` ngay từ đầu |
| Dùng Java 23 vì project khác đang dùng | Dùng Java 21 LTS |
| Nâng Spring Boot 4.x trong Sprint 1 | Dùng Spring Boot 3.5.x với patch đã được verify |
| Tạo server-rendered UI trong backend | Backend chỉ REST API |
| Dùng Hibernate auto-update schema | Dùng Flyway + `ddl-auto=validate` |
| Thêm Redis/Kafka ngay khi chưa có use case | Defer đến khi cần |
| Copy domain OASIS sang MAR | Map pattern, viết domain MAR |

## 10. TESTING CONVENTIONS

### 10.1. Test framework

| Layer | Tool |
|---|---|
| Unit | JUnit 5, Mockito |
| API | Spring Boot Test, MockMvc |
| Security | spring-security-test |
| Repository | Testcontainers PostgreSQL |
| Migration | Flyway on fresh PostgreSQL container |

### 10.2. Baseline compliance test

Project bootstrap must prove:

- App starts with active `local` profile.
- Flyway can run on fresh DB.
- Actuator health works.
- Security filter chain is active.
- `/api/v1` endpoint prefix is used.

### 10.3. Không dùng H2 cho DB-specific behavior

Do not use H2 for:

- JSONB.
- PostgreSQL indexes.
- Flyway dialect validation.
- Tenant query behavior that relies on PostgreSQL syntax.

## 11. CODE REVIEW CHECKLIST

### 11.1. Baseline compliance

- [ ] Java 21.
- [ ] Spring Boot 3.5.x, patch version đã được Tech Lead verify.
- [ ] Maven Wrapper exists.
- [ ] Root package is `vn.mar`.
- [ ] Package tree có `role`, `common.tenant`, `common.time` đúng convention hoặc được đánh reserved rõ.
- [ ] Backend is REST API only.
- [ ] PostgreSQL driver and Flyway configured.
- [ ] `ddl-auto=validate`.

### 11.2. Architecture boundary

- [ ] No server-rendered UI package.
- [ ] No OASIS package/domain copied.
- [ ] No new dependency without approval.
- [ ] No Redis/Kafka unless justified.

### 11.3. Quality

- [ ] App starts locally.
- [ ] Fresh DB migration passes.
- [ ] Local/QA profile và seed runner không phụ thuộc thao tác tay.
- [ ] Minimal audit có cho sensitive Sprint 1 changes.
- [ ] Basic health endpoint works.
- [ ] Sensitive config is not hard-coded.

## 12. TÀI LIỆU LIÊN QUAN

- `README.md` - Index bộ convention MAR.
- `02-coding-package-convention.md` - Package/module detail.
- `03-rest-api-convention.md` - REST API detail.
- `04-database-flyway-convention.md` - DB/Flyway detail.
- `05-security-auth-authz-convention.md` - Security/auth detail.
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\MAR_ARCHITECTURE_VERSION_CONVENTION.md` - Baseline đã chốt trước đó.
- `D:\Documents-for-Expert-Design-Database\MAR\docs BA\13-r1a-sprint-1-signoff-decision-log.md` - Sign-off decision log.

## 13. LỊCH SỬ CẬP NHẬT

| Ngày | Phiên bản | Nội dung | Người sửa |
|---|---|---|---|
| 30/06/2026 | MAR-CONV-1.0 | Đồng bộ package tree với coding/API convention, nới wording Spring Boot patch, thêm local/QA baseline và minimal audit mandatory cho Sprint 1 | Tech Lead / SA |
| 30/06/2026 | MAR-CONV-1.0 | Tạo architecture baseline convention đầy đủ cho MAR, map từ OASIS nhưng khóa Java 21, Spring Boot 3.5.x, PostgreSQL 17, Flyway, REST API, `vn.mar` | Tech Lead / SA |
