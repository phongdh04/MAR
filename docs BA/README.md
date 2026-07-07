# README - MAR docs BA

> Phiên bản cập nhật: `v2.4 - Platform tenant permission ADR - 2026-07-07`.
> Baseline kỹ thuật hiện hành: `Java 21 + Spring Boot ecosystem`, `PostgreSQL 17`, `Flyway`, `Spring Data JPA/Hibernate`, `Docker Compose local/QA`.
> Ghi chú: technical baseline đã freeze tại file `18-r1a-dev-baseline-freeze.md`; dev kickoff package đã tạo tại `19-r1a-dev-kickoff-package.md`.
## 1. Công dụng thư mục

Thư mục này chứa bộ tài liệu BA cho dự án **MAR Lead-to-Enrollment MVP**: từ hiểu yêu cầu, epic brief, story specs, technical BA baseline, API/DB/wireframe, backlog, Sprint 1 ready package, QA pack, sign-off/kickoff và project kickoff summary.

Trạng thái hiện tại: **Ready to start backend bootstrap**. Backend ecosystem đã được user chọn là **Spring Boot ecosystem** và local/QA environment đã chốt dùng **Docker Compose**.

Development bootstrap có thể bắt đầu sau baseline freeze này. Release acceptance vẫn chỉ được pass sau khi có code/migration/test evidence.

Baseline freeze và dev kickoff đã được ghi tại:

- `13-r1a-sprint-1-signoff-decision-log.md`
- `14-r1a-sprint-1-signoff-kickoff-pack.md`
- `16-techlead-sa-decision-register.md`
- `17-techlead-sa-decision-rationale.md`
- `18-r1a-dev-baseline-freeze.md`
- `19-r1a-dev-kickoff-package.md`
- `20-r1a-platform-tenant-permission-adr.md`

## 2. Cách đọc nhanh

| Mục tiêu | Nên đọc |
|---|---|
| Hiểu dự án và MVP | `brief.md`, `15-project-kickoff-summary.md` |
| Hiểu quyết định nghiệp vụ ban đầu | `00-ba-project-understanding.md`, `01-ba-clarification-questions.md` |
| Hiểu Release 1 story | `03-r1-story-specs.md` |
| Hiểu R1A technical/API/DB/UX baseline | `04`, `05`, `06`, `07` |
| Chuẩn bị dev Sprint 1 | `09`, `10`, `11`, `18`, `19` |
| Chuẩn bị QA/sign-off Sprint 1 | `12`, `13`, `14` |
| Chốt kỹ thuật dưới vai Tech Lead/SA | `16`, `17`, `18` |

Quy tắc đọc: file càng về sau càng gần execution hơn. Nếu có lệch giữa brief/draft cũ và tài liệu Sprint đã sign-off, dùng tài liệu Sprint/sign-off làm baseline gần nhất và cập nhật traceability.

## 3. File index và vấn đề cần chốt

| File | Công dụng | Cần chốt / lưu ý |
|---|---|---|
| [brief.md](brief.md) | Epic Brief chính thức cho MAR Lead-to-Enrollment MVP; định vị sản phẩm, scope MVP, entity, KPI, DoD. | Đã approved ở mức Epic Brief; không dùng một mình để dev code. Implementation phải theo các downstream docs đã sign-off. |
| [00-ba-project-understanding.md](00-ba-project-understanding.md) | Ghi nhận hiểu biết BA về dự án sau khi đọc tài liệu gốc và feedback. | Không có blocker; dùng làm bối cảnh, không phải spec triển khai. |
| [01-ba-clarification-questions.md](01-ba-clarification-questions.md) | Decision Log DEC-01 đến DEC-23 và câu hỏi làm rõ nghiệp vụ. | Baseline đã đủ cho Epic Brief; các câu hỏi chi tiết còn lại phải được xác nhận lại theo từng story nếu ảnh hưởng dev. |
| [02-draft-epic-scope.md](02-draft-epic-scope.md) | Draft phạm vi Epic trước khi ra `brief.md`; hữu ích để xem lịch sử scope. | Đã được supersede bởi `brief.md` và story specs; không dùng làm baseline mới nhất. |
| [03-r1-story-specs.md](03-r1-story-specs.md) | Story Specs baseline cho Release 1, chia R1A/R1B/R1C và các epic/story chính. | Từng story vẫn cần PO/Tech xác nhận API, UI, data, security và effort trước khi dev. |
| [04-r1a-technical-ba-spec.md](04-r1a-technical-ba-spec.md) | Technical BA spec cho R1A Lead & Pipeline Core: nghiệp vụ, data, validation, permission, test scenarios. | Ready for PO/Tech grooming, chưa phải final technical design. Cần Tech Lead/SA chốt kiến trúc triển khai. |
| [05-r1a-api-contract.md](05-r1a-api-contract.md) | API contract baseline cho R1A: endpoint, request/response, validation, permission, events, error. | Chưa phải OpenAPI final. Cần đồng bộ với `SP1-D09` API error envelope và framework/backend implementation. |
| [06-r1a-db-schema-erd.md](06-r1a-db-schema-erd.md) | DB/schema baseline và ERD cho R1A. | Chưa phải DDL final. `SP1-D01` đã freeze theo MAR-CONV-1.1/dev baseline; migration thật phải theo PostgreSQL 17 + Docker Compose local/QA + Flyway naming `VYYYYMMDD_NN__...`. |
| [07-r1a-wireframe-checklist.md](07-r1a-wireframe-checklist.md) | Checklist màn hình, state và UX flow cho R1A. | Cần UX/FE xác nhận route, layout, screen priority và scope UI trước dev. |
| [08-r1a-grooming-review.md](08-r1a-grooming-review.md) | Review/grooming pack cho R1A, liệt kê quyết định đề xuất, risk và Go/No-Go. | Nhiều decision ở trạng thái proposed; dùng để trace, nhưng Sprint 1 phải theo file `13` và `14`. |
| [09-r1a-dev-backlog.md](09-r1a-dev-backlog.md) | Backlog dev đề xuất cho R1A, chia slice và dependency. | Draft backlog, chưa sprint-ready. Cần estimate, owner và sprint planning trước commitment. |
| [10-r1a-sprint-1-ready-package.md](10-r1a-sprint-1-ready-package.md) | Gói ticket Sprint 1 Foundation Setup: candidate items, assumptions, DoR/DoD, risks, demo/exit criteria. | Cần chốt D01-D10, UX final và effort trước khi đưa vào sprint chính thức. |
| [11-r1a-sprint-1-technical-handoff.md](11-r1a-sprint-1-technical-handoff.md) | Handoff kỹ thuật Sprint 1 cho Tech Lead/SA/BE/FE/QA. | Superseded bởi baseline freeze `18` và dev kickoff `19` cho phần execution; vẫn dùng để trace handoff gốc. |
| [12-r1a-sprint-1-qa-acceptance-pack.md](12-r1a-sprint-1-qa-acceptance-pack.md) | QA acceptance pack: API tests, UI smoke, security/tenant isolation, release gate, demo acceptance. | Pass condition đã rõ; actual result chỉ có sau implementation. Cần QA env, Tenant A/B data và import fixture/API. |
| [13-r1a-sprint-1-signoff-decision-log.md](13-r1a-sprint-1-signoff-decision-log.md) | Sign-off checklist và decision log trước khi dev commit Sprint 1. | Phải điền final decision cho `SP1-D01` đến `SP1-D10`, release gate status và approver sign-off. |
| [14-r1a-sprint-1-signoff-kickoff-pack.md](14-r1a-sprint-1-signoff-kickoff-pack.md) | Agenda/decision ballot/release gate ballot để chạy buổi Sprint 1 sign-off/kickoff. | Cần chạy meeting thật, ghi `Go / Conditional Go / No-Go`, action items, blocker và post-meeting status. |
| [15-project-kickoff-summary.md](15-project-kickoff-summary.md) | Kickoff summary ngắn: overview, scope, effort sơ bộ và tech stack baseline. | Effort là BA-level estimate; backend/version/package/Flyway convention đã freeze ở `18`/`19`; frontend framework vẫn cần FE/Tech Lead chốt riêng nếu đi vào FE implementation. |
| [16-techlead-sa-decision-register.md](16-techlead-sa-decision-register.md) | Decision register dưới vai Tech Lead/SA, gom các điểm cần chốt và đề xuất kỹ thuật/kiến trúc. | Dùng để chốt P0 trước dev và P1/P2 trước các slice R1A tương ứng. |
| [17-techlead-sa-decision-rationale.md](17-techlead-sa-decision-rationale.md) | Giải thích vì sao chốt từng decision, vì sao không chọn option khác và map với hiện trạng workspace/docs. | Dùng kèm file `16` trong buổi chốt kỹ thuật/kiến trúc. |
| [18-r1a-dev-baseline-freeze.md](18-r1a-dev-baseline-freeze.md) | Biên bản freeze technical baseline trước dev kickoff/backend bootstrap. | Source of truth hiện tại cho quyết định Docker local/QA, readiness status và next implementation order. |
| [19-r1a-dev-kickoff-package.md](19-r1a-dev-kickoff-package.md) | Gói kickoff triển khai backend bootstrap/foundation: ticket order, owner role, evidence, Docker/DB/Flyway contract. | File cần đọc ngay trước khi tạo backend repo/code. |
| [20-r1a-platform-tenant-permission-adr.md](20-r1a-platform-tenant-permission-adr.md) | ADR chốt tenant creation là platform/bootstrap-level và đề xuất permission `platform.tenant.manage`. | P2b đang blocked cho tới khi có bootstrap/platform actor seed evidence; sau khi implement thì không dùng `tenant.manage` cho create tenant. |

## 4. Các vấn đề cần chốt nổi bật trước dev

| ID | Vấn đề | File liên quan |
|---|---|---|
| SP1-D01 | MAR-CONV-1.1 locked; bootstrap theo Java 21, Spring Boot 3.5.x exact patch verified during bootstrap, PostgreSQL 17, Docker Compose local/QA, Flyway, `vn.mar`, REST API only/no Thymeleaf | `13`, `14`, `15`, `16`, `17`, `18`, `our architecture/README.md` |
| SP1-D07 | Auth/session tenant context hoặc `R1A-TECH-001` | `10`, `11`, `12`, `13`, `14` |
| SP1-D08 | Enum convention `UPPER_SNAKE_CASE` và nơi lưu source of truth | `05`, `06`, `11`, `13`, `14` |
| SP1-D09 | API error envelope và common contract | `05`, `12`, `13`, `14` |
| SP1-D10 | Import foundation testability: BE fixture command là default; internal draft API optional local/QA only | `10`, `12`, `13`, `14`, `15`, `18` |
| Effort | Team estimate lại effort trước sprint commitment | `09`, `10`, `15` |
| Final Go/No-Go | Development commitment decision | `13`, `14` |
| Docker baseline | Local/QA dùng Docker Compose PostgreSQL 17 + app + seed/fixture runner | `16`, `17`, `18`, `our architecture/12` |
| Dev kickoff | Ticket order, owner role, test/evidence và next implementation action | `18`, `19`, `our architecture/09`, `our architecture/12` |
| Platform tenant permission | ADR đã chốt hướng `platform.tenant.manage`, nhưng code change phải chờ bootstrap/platform actor seed evidence | `16`, `17`, `20`, `our architecture/03`, `our architecture/05` |

## 5. Trạng thái sử dụng hiện tại

| Việc muốn làm | Trạng thái |
|---|---|
| Dùng để hiểu sản phẩm/MVP | Được |
| Dùng để tổ chức Sprint 1 sign-off/kickoff | Được |
| Dùng để dev code ngay | Được cho backend bootstrap/foundation implementation, theo file `19` |
| Dùng làm baseline sau khi sign-off Go | Được; file `13`, `14`, `16`, `17`, `18` đã cập nhật baseline freeze |
