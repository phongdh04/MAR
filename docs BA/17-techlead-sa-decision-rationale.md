# Tech Lead / SA Decision Rationale

> Phiên bản cập nhật: `v2.3 - Platform tenant permission ADR - 2026-07-07`.
> Baseline kỹ thuật hiện hành: `Java 21 + Spring Boot ecosystem`, `PostgreSQL 17`, `Flyway`, `Spring Data JPA/Hibernate`, `Docker Compose local/QA`.
> Ghi chú: technical baseline đã freeze; Docker được user chốt làm local/QA environment baseline.
## 1. Mục đích

Tài liệu này giải thích **vì sao** Tech Lead/SA đề xuất các quyết định trong `16-techlead-sa-decision-register.md`, đồng thời map từng quyết định với hiện trạng dự án.

Nguyên tắc đọc:

- Đây là rationale kỹ thuật, không thay thế sign-off.
- Quyết định cuối vẫn phải được ghi vào `13-r1a-sprint-1-signoff-decision-log.md` và `14-r1a-sprint-1-signoff-kickoff-pack.md`.
- User đã chọn Spring Boot ecosystem làm backend baseline. Nếu sau này đổi stack, phải có decision mới và vẫn giữ nguyên các nguyên tắc: tenant isolation, API-level permission, audit, testability, traceability.

## 2. Hiện trạng dự án đang map vào quyết định

| Hiện trạng | Ý nghĩa với Tech Lead/SA |
|---|---|
| MAR hiện chủ yếu là bộ BA docs, chưa có codebase MAR riêng | Không nên giả định framework đã chốt; mọi stack decision vẫn cần sign-off |
| Workspace có `DATN_backend` và `backend_hackathon` dùng Java/Spring Boot, PostgreSQL, JPA, Security, Redis; `backend_hackathon` có Flyway | Spring Boot + PostgreSQL + Flyway/JPA khớp hiện trạng và đã được User chọn làm backend baseline |
| BA docs yêu cầu tenant isolation, permission matrix, AuditLog, import history, enum/error contract | Cần backend framework có cấu trúc rõ, migration thật, security layer và testability tốt |
| Sprint 1 chỉ là foundation setup | Không cần microservice/event streaming phức tạp ngay; ưu tiên monolith modular dễ kiểm thử |
| R1A sau Sprint 1 có webhook/import/dedup/assignment/SLA | Schema phải future-ready, có idempotency, activity log, working hours, integration event |

## 3. Rationale công nghệ nền

| Chốt | Đề xuất | Vì sao chọn | Vì sao không chọn option khác | Điều kiện đổi |
|---|---|---|---|---|
| Backend framework | Spring Boot ecosystem | User đã chọn Spring Boot; workspace đã có Spring Boot/PostgreSQL/Security/JPA/Flyway pattern; MAR cần security, transaction, migration, validation, audit ổn định | Không chọn Express/Fastify thuần vì thiếu structure cho permission/audit lớn. Không chọn NestJS vì User đã chốt Spring Boot và workspace hiện nghiêng Java. Không chọn Django/Laravel/FastAPI vì không có nền đó trong workspace | Đổi stack chỉ khi có decision mới |
| Frontend framework | React + TypeScript; Vite SPA cho admin Sprint 1, Next.js nếu cần routing/SSR sau | Sprint 1 là admin setup/catalog UI, cần UI nhanh, testable; workspace có React/Vite sample | Không chọn Angular vì nặng hơn cho MVP nhỏ. Không chọn Vue nếu team/frontend baseline đang React. Không cần Next.js nếu chỉ là admin SPA không SSR | Đổi sang Next.js nếu roadmap cần app router, SSR, SEO/public pages hoặc BFF |
| Database | PostgreSQL | Docs yêu cầu JSONB cho mapping/raw row, multi-tenant indexes, audit/history, relational integrity | Không chọn MongoDB vì nghiệp vụ có quan hệ mạnh: tenant, user, permission, catalog, import rows. Không chọn MySQL nếu cần JSONB/index linh hoạt và hiện workspace đã dùng PostgreSQL. Không chọn SQLite cho multi-tenant/staging | Đổi DB chỉ khi infra/company standard bắt buộc |
| Migration tool | Flyway | Sprint 1 cần migration repeatable, QA fresh env, seed rõ; `backend_hackathon` đã có Flyway; Flyway đi rất tự nhiên với Spring Boot | Không dùng Hibernate `ddl-auto=update` cho production vì không kiểm soát schema drift. Không dùng manual SQL rời rạc vì khó trace. Không chọn Prisma Migrate vì backend đã chốt Spring Boot. Liquibase mạnh nhưng verbose hơn Flyway cho Sprint 1 | Dùng Liquibase nếu cần rollback/change management phức tạp |
| ORM/data access | Spring Data JPA/Hibernate + native query khi cần | CRUD/config/catalog phù hợp JPA; audit/import/history vẫn cần kiểm soát transaction; native query xử lý report/JSONB khi ORM không tiện | Không raw SQL toàn bộ vì tăng effort và bug mapping. Không ORM-only cứng nhắc cho report phức tạp sau này. Không dùng Prisma vì backend không còn là TS baseline | Thêm jOOQ/native query khi dashboard/report cần query tối ưu |
| API style | REST `/api/v1` + OpenAPI | BA docs đã viết theo REST; QA dễ test; FE admin CRUD/list/detail phù hợp REST | Không chọn GraphQL vì scope R1A chưa cần query graph phức tạp, QA/error contract khó hơn. Không chọn gRPC vì FE/browser và external webhook không phù hợp | Xem lại GraphQL khi dashboard/inbox cần query dynamic nhiều chiều |
| Local/QA env | Docker Compose cho PostgreSQL 17 + app + seed/fixture runner | User đã chốt dùng Docker; QA pack cần fresh migration, Tenant A/B, demo/test seed; Docker giúp DB local/QA repeatable | Không rely DB local thủ công vì test không repeatable. Không đưa Kubernetes ngay vì overkill cho Sprint 1. Không tạo schema tay vì Flyway là source of truth | Dùng Kubernetes sau khi deployment scale rõ |

## 4. Rationale cho P0 - phải chốt trước dev Sprint 1

| ID | Chốt | Map hiện trạng dự án | Vì sao chốt như vậy | Vì sao không chọn cách khác |
|---|---|---|---|---|
| P0-01 | Spring Boot/PostgreSQL/Flyway baseline | Docs `06/11/13/14/15` cần version/convention trước dev commit; workspace đang dùng PostgreSQL, Flyway có trong `backend_hackathon`; architecture OASIS có nhiều Spring Boot convention nhưng còn Thymeleaf/MS SQL/`com.oasis.*` cần override | Chốt MAR-CONV-1.1: Java 21, Spring Boot 3.5.x exact patch verified during bootstrap, PostgreSQL 17, Docker Compose local/QA, Flyway naming `VYYYYMMDD_NN__...`, JPA/Hibernate, root package `vn.mar`, REST API only/no Thymeleaf | Không chỉ chốt PostgreSQL vì vẫn thiếu migration/package/API convention. Không dùng `ddl-auto=update` vì không đủ kiểm soát production/staging. Không chọn Prisma Migrate vì backend không phải TS baseline. Không chọn Thymeleaf vì MAR cần API-first/web admin riêng |
| P0-02 | Backend framework | MAR chưa có codebase riêng; workspace có Java/Spring Boot foundation; User đã chọn Spring Boot | Chốt Spring Boot ecosystem. Giảm rủi ro team phải đổi stack, tận dụng security/JPA/Flyway/PostgreSQL pattern | Không chốt NestJS vì trái decision mới. Không dùng Express/Fastify thuần vì Sprint 1 cần structure cho tenant/security/audit |
| P0-03 | Frontend framework | Sprint 1 cần admin setup/catalog UI; workspace có React/Vite sample | React + TypeScript đủ phổ biến và nhanh. Vite SPA phù hợp admin nội bộ | Không cần Angular vì nặng. Không bắt Next.js nếu không cần SSR/public SEO. Không chọn framework khác nếu không có lợi rõ |
| P0-04 | Auth/session tenant context | Permission/tenant isolation trong `10/11/12/13/14` phụ thuộc `tenant_id`, `actor_id`, `role_code` | Nếu auth chưa có, `R1A-TECH-001` phải là ticket đầu tiên để tất cả API có context thống nhất | Không để từng API tự parse tenant/role vì dễ leak cross-tenant. Không test permission nếu chưa có actor context |
| P0-05 | Multi-tenant isolation | MAR là SaaS/pilot nhiều trung tâm; QA pack yêu cầu Tenant A/B và detail-level isolation | Mọi bảng nghiệp vụ có `tenant_id`, mọi query/API filter tenant, detail endpoint phải chặn cross-tenant | Không chỉ filter ở list API vì detail endpoint dễ leak. Không rely UI hide vì direct API vẫn bypass được |
| P0-06 | API-level permission guardrail | Permission matrix là P0; QA test direct API bypass | Enforce ở guard/service layer, UI chỉ là presentation | Không chỉ disable button ở UI vì user có thể gọi API trực tiếp. Không hard-code quyền rải rác vì khó audit |
| P0-07 | Minimal AuditLog | Permission change và sensitive setup changes phải truy vết; docs đã chuyển AuditLog thành Must | BA term là `AuditLog`, implementation dùng append-only `audit_events` trong Sprint 1, ghi actor/entity/action/reason/time/request_id | Không để audit sau vì permission matrix không thể nghiệm thu an toàn. Không log text file vì không query/audit được. Không tạo song song `audit_logs` và `audit_events` |
| P0-08 | API error envelope | QA/FE cần format lỗi thống nhất; `12/13/14` đã yêu cầu | Dùng `{ error: { code, message, details[] }, meta: { request_id } }` cho validation/permission/conflict | Không trả lỗi tùy endpoint vì FE khó xử lý, QA khó assert. Không chỉ message string vì thiếu code/details |
| P0-09 | Enum source of truth | Trước đó từng lệch `Active/SalesLead`; docs đã chốt `UPPER_SNAKE_CASE` | DB/API enum key dùng `UPPER_SNAKE_CASE`, UI label/localization tách riêng | Không dùng label UI làm enum vì đổi ngôn ngữ sẽ phá API/DB. Không dùng CamelCase lẫn lộn vì FE/BE/QA dễ lệch |
| P0-10 | Import foundation testability | Sprint 1 chưa làm preview/confirm, nhưng QA phải test ImportBatch/ImportRow/history | Dùng BE fixture command là default; chỉ làm internal draft API nếu FE demo cần và chỉ local/QA | Không mở public create batch API nếu chưa có nghiệp vụ thật. Không để QA seed tay DB vì không repeatable |
| P0-11 | Seed strategy | QA/demo cần data; production migration không được chứa demo tenant | Tách system seed, demo seed, test seed; owner rõ | Không trộn demo seed vào production migration. Không để QA tự tạo thủ công vì test không ổn định |
| P0-12 | GET/list/detail APIs | FE admin form và QA cross-tenant detail cần endpoint thật | Include list/detail cho tenant, branch, user, permission, catalog, import history | Không chỉ POST/PATCH vì FE không load được edit form. Không chỉ list vì detail isolation chưa test được |
| P0-13 | Team entity/scope | Team entity chưa chắc cần trong pilot; docs chốt defer | Không thêm Team table Sprint 1; reserve `TEAM` scope disabled để không phá permission model sau này | Không thêm Team sớm vì tăng schema/UX/test scope. Không bỏ hẳn `TEAM` scope vì sau này mở rộng khó hơn |
| P0-14 | Sprint 1 non-goals | Sprint 1 là foundation; PO dễ kỳ vọng nhầm import/dedup/pipeline | PO phải xác nhận không claim Sprint 2/R1A nghiệp vụ đầy đủ | Không demo “giống hoàn tất” các feature chưa làm vì tạo sai kỳ vọng và QA không có basis nghiệm thu |
| P0-15 | Final Go/No-Go | `13/14` là cửa dev commitment | Chỉ Go khi D01-D10 approved và release gate pass condition accepted | Không cho dev code trước sign-off vì các decision nền đổi sẽ gây sửa dây chuyền |
| P0-17 | Platform tenant creation permission | Code hiện có `POST /api/v1/tenants` dùng `tenant.manage`, trong khi convention đã nói tenant creation là platform/bootstrap-level | Chốt ADR `20`: hướng đúng là dùng `platform.tenant.manage` cho create tenant; giữ `tenant.manage` cho update/get tenant-scoped. Chưa implement P2b trong batch này vì thiếu bootstrap/platform actor seed evidence | Không tiếp tục dùng `tenant.manage` dài hạn vì dễ trao quyền tạo tenant cho tenant admin. Không hard-code role `ADMIN/CEO` vì trái permission-based authz. Không đổi ngay sang platform permission table riêng vì scope lớn hơn remediation hiện tại |

## 5. Rationale cho P1 - chốt trước các slice R1A sau Sprint 1

| ID | Chốt | Map hiện trạng | Vì sao chốt như vậy | Vì sao không chọn cách khác |
|---|---|---|---|---|
| P1-01 | CustomerIdentity | Dedup theo phone/email/Zalo trong `brief/06/08` | Cần bảng identity riêng để một customer có nhiều phone/email/Zalo | Không unique cứng trên CustomerProfile vì phụ huynh/người học có thể dùng chung contact |
| P1-02 | Duplicate email exact nhưng phone khác | Dễ merge sai lead | Tạo DuplicateCase `NEEDS_REVIEW`, chưa đưa thẳng vào pipeline | Không auto-merge vì rủi ro gộp sai khách |
| P1-03 | Strict unique phone/email | Pilot có phụ huynh/học viên dùng chung số | Dùng CustomerIdentity + service dedup | Không strict DB unique trên customer vì sẽ chặn case hợp lệ |
| P1-04 | Merge/unmerge permission | Unmerge có rủi ro phá dữ liệu | Admin-only unmerge P0 | Không cho Sales Lead unmerge sớm vì cần audit/trách nhiệm cao |
| P1-05 | Activity/InteractionLog | SLA/contact KPI không thể suy từ stage | ActivityLog là source of truth cho call/message/note | Không dùng stage `Contacting/Contacted` để tính KPI vì dễ sai và không công bằng |
| P1-06 | SLA hit vs contact success | Docs đã tách first response và contact success | SLA hit = first outbound activity đúng hạn; contact success = connected/replied | Không gộp thành “contacted within SLA” vì advisor gọi đúng hạn nhưng khách không nghe vẫn bị tính sai |
| P1-07 | WorkingHours/SLA policy | Tenant timezone/ngoài giờ ảnh hưởng due_at | Có config tối thiểu + default pilot | Không hard-code toàn hệ thống vì mỗi trung tâm có giờ làm khác |
| P1-08 | Stage transition enforcement | Pipeline là nghiệp vụ lõi R1A | Centralize ở service | Không để UI quyết định transition vì API bypass sẽ sai |
| P1-09 | Assignment workload | Sprint đầu cần đơn giản | Realtime count đơn giản, tối ưu snapshot sau | Không build snapshot phức tạp ngay khi chưa có volume |
| P1-10 | Assignment history/pool state | Round-robin/reassign cần audit | Có history và pool state | Không tính round-robin từ dữ liệu hiện tại mỗi lần vì dễ lệch khi concurrency |
| P1-11 | Import preview sync/async | File nhỏ/lớn có nhu cầu khác nhau | Nhỏ sync, lớn async + polling | Không ép tất cả sync vì file lớn timeout; không ép tất cả async vì MVP demo chậm hơn |
| P1-12 | CSV vs Google Sheet | CSV là P0 rõ nhất | CSV must-have, Sheet should-have, no live sync | Không live sync sớm vì tăng auth/scheduling/error handling |
| P1-13 | Webhook sync/async | Pilot volume nhỏ nhưng cần mở rộng | Sync 200 nếu nhỏ, async 202 nếu có queue | Không hứa `lead_id` nếu xử lý async chưa xong |
| P1-14 | Idempotency store | Webhook/import dễ duplicate | Dùng integration_events/external id/idempotency key | Không dùng raw payload thay idempotency vì payload có thể đổi format hoặc masked |
| P1-15 | Raw payload storage | Debug cần payload nhưng PII nhạy cảm | Retention ngắn, sanitized/masked, hash giữ lâu hơn | Không lưu raw dài hạn mặc định vì rủi ro PII |
| P1-16 | Integration/Webhook log | Admin/Marketing cần debug processed/failed/duplicate | Có integration log riêng | Không chỉ ghi app log vì user vận hành không xem được |
| P1-17 | Lead source required | Attribution R1C cần source | Marketing lead bắt buộc; import/manual cũ warning + `UNKNOWN_SOURCE` | Không reject toàn bộ lead cũ thiếu source vì mất dữ liệu lịch sử |
| P1-18 | GraphQL hay REST | Docs/API/QA đang theo REST | Giữ REST R1A | Không dùng GraphQL sớm vì tăng contract/test complexity |
| P1-19 | Soft delete/inactive | Config nên giữ lịch sử | Dùng `INACTIVE`, history/audit append-only | Không hard delete nghiệp vụ vì mất traceability |
| P1-20 | Partition audit/message sau này | Retention tối thiểu 24 tháng | Chưa partition ngay nhưng schema không cản | Không partition sớm vì over-engineering khi chưa có volume |

## 6. Rationale UX/FE quan trọng

| Chốt | Đề xuất | Vì sao | Không chọn |
|---|---|---|---|
| Admin setup route/layout | Form/list đơn giản | Sprint 1 cần setup nhanh, testable | Không làm UI phức tạp/card-heavy vì admin tool cần hiệu quả |
| Permission matrix UX | Grid edit + guardrail disabled | Admin cần chỉnh nhanh; QA thấy guardrail rõ | Không modal nhiều bước nếu làm chậm thao tác P0 |
| Catalog UI | Tree nếu effort thấp; tabs/list fallback | Cấu trúc language -> program -> course tự nhiên | Không ép tree nếu FE effort vượt Sprint 1 |
| Advisor inbox | Table/list | SLA scanning cần dense UI | Không kanban sớm vì đẹp nhưng khó scan SLA hàng loạt |
| Integration retry | View/debug trước | Retry cần idempotency chắc | Không bật retry nếu chưa an toàn duplicate |

## 7. Cập nhật khuyến nghị so với file 16 trước đó

File `16` đã được cập nhật để phản ánh decision mới:

- Trước đó ghi generic greenfield: `NestJS + TypeScript`.
- Sau khi map workspace, primary backend được đổi sang **Spring Boot** nếu MAR reuse code/team/process hiện có.
- Sau decision mới của User, **Spring Boot ecosystem** là selected baseline.
- NestJS chỉ còn là alternative nếu có decision đổi stack trong tương lai.

Kết luận SA: baseline đã freeze cho backend bootstrap là **Java 21 + Spring Boot 3.5.x exact patch verified during bootstrap + PostgreSQL 17 + Docker Compose local/QA + Flyway + Spring Data JPA/Hibernate**, root package `vn.mar`, REST API only và **không dùng Thymeleaf**.

DB instance local/QA sẽ được dựng bằng Docker Compose; schema/tables sẽ do Flyway migration tạo, không tạo tay trong database. Phần còn lại cần implementation evidence là Spring Boot bootstrap, Docker PostgreSQL running, Flyway baseline, Local JWT/request context, tenant isolation tests, permission guardrail, `audit_events`, error envelope và import fixture command.
