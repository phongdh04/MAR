# IMPORT, FILE & STORAGE CONVENTION - QUY TẮC IMPORT/FILE MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Spring Web Multipart, PostgreSQL JSONB, Flyway, storage abstraction, S3-compatible storage optional later  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\fileupload_convention.md` - Pattern file upload/storage OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\database_convention.md` - JSONB/import table pattern
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\audit_convention.md` - Audit import/file event
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\04-database-flyway-convention.md` - PostgreSQL/Flyway MAR

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa import foundation, file upload và storage cho MAR.

Mục đích:

- Tạo nền dữ liệu `import_batches` / `import_rows` đúng để các sprint sau phát triển lead import.
- Tránh claim Sprint 1 đã có full parser/preview/confirm khi scope hiện tại mới là foundation.
- Chuẩn hóa cách lưu raw row, normalized row, mapping config và row errors bằng JSONB.
- Chuẩn bị file storage abstraction nếu upload file thật được đưa vào scope.
- Đảm bảo tenant isolation, security, audit và testability cho import.

Nguyên tắc ghi nhớ:

> **"Import foundation lưu sự thật đủ kiểm tra; parser production chỉ được hứa khi đã có contract và test."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Import batch/row data model.
- Import history/error API.
- Mapping config JSONB.
- File metadata nếu có upload.
- Storage abstraction nếu lưu file thật.
- CSV/Excel/Google Sheet/Meta Lead Ads foundation.
- Import audit/security/test.

Không áp dụng cho Sprint 1 nếu chưa được approve:

- Production CSV/Excel parser hoàn chỉnh.
- Google Sheet connector.
- Meta Lead Ads connector.
- Import preview/confirm UX đầy đủ.
- Dedup lead/customer/opportunity creation.
- Virus scan pipeline production.
- Object storage production deployment.

## 3. NGUYÊN TẮC CHUNG

1. **Foundation first:** Sprint 1 chỉ chốt model/testability nếu parser chưa vào scope.
2. **Tenant-scoped import:** mọi batch/row query phải có `tenant_id`.
3. **JSONB for flexible row:** raw/normalized/error/mapping dùng JSONB.
4. **Relational for stable concepts:** tenant, branch, catalog, user không giấu trong JSONB.
5. **No binary in DB:** file thật lưu object storage/local dev, DB chỉ lưu metadata/object key.
6. **Validate before store:** upload thật phải validate size, extension, MIME/magic number.
7. **Do not trust filename:** original filename chỉ để hiển thị, stored object key do server generate.
8. **No raw PII logging:** không log full raw row/file content.
9. **Audit import milestones:** batch created/confirmed/completed cần audit theo scope.
10. **Do not oversell:** demo/docs phải nói rõ parser/connector nào đã implement, cái nào chưa.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Import entity/table naming

| Entity | Table | PK |
|---|---|---|
| `ImportBatch` | `import_batches` | `import_batch_id` |
| `ImportRow` | `import_rows` | `import_row_id` |
| `FileMetadata` | `file_metadata` | `file_metadata_id` |

### 4.2. Import enum naming

Import type:

```text
LEAD
```

Source type:

```text
CSV
EXCEL
GOOGLE_SHEET
WEBSITE_FORM
META_LEAD_ADS
MANUAL
OTHER
```

Batch status:

```text
DRAFT
UPLOADED
VALIDATING
VALIDATED
PREVIEWED
CONFIRMED
IMPORTING
COMPLETED
FAILED
CANCELLED
EXPIRED
```

Row status:

```text
PENDING
VALID
ERROR
DUPLICATE
SKIPPED
IMPORTED
```

Sprint 1 có thể chỉ dùng subset, nhưng enum/reserved lifecycle phải nhất quán.

### 4.3. File naming

Stored object key không dùng client filename.

Pattern:

```text
tenant/{tenant_id}/import/{import_batch_id}/{uuid}.{ext}
```

Ví dụ:

```text
tenant/00000000-0000-0000-0000-000000000001/import/11111111-1111-1111-1111-111111111111/9fd8.csv
```

Original filename lưu metadata:

```text
lead_june_2026.csv
```

### 4.4. Class naming

| Loại | Convention | Ví dụ |
|---|---|---|
| Controller | `<Resource>Controller` | `LeadImportController` |
| Service | `<UseCase>Service` | `CreateImportBatchService` |
| Parser | `<SourceType>ImportParser` | `CsvLeadImportParser` |
| Validator | `<Entity>Validator` | `ImportBatchValidator` |
| Storage | `FileStorage` interface | `S3FileStorage`, `LocalFileStorage` |
| DTO request | `<Action>Import<Request>` | `CreateLeadImportBatchRequest` |
| DTO response | `<Entity>Response` | `ImportBatchDetailResponse` |

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Import package

```text
vn.mar.leadimport
├── controller
│   └── LeadImportController.java
├── dto
│   ├── request
│   └── response
├── entity
│   ├── ImportBatch.java
│   └── ImportRow.java
├── repository
│   ├── ImportBatchRepository.java
│   └── ImportRowRepository.java
├── service
│   ├── ImportBatchService.java
│   ├── ImportRowService.java
│   └── ImportValidationService.java
├── mapper
├── validator
└── parser
```

### 5.2. File package

Chỉ tạo khi có upload/storage thật:

```text
vn.mar.common.file
├── FileStorage.java
├── FileValidator.java
├── FileMetadata.java
├── FileMetadataRepository.java
├── FileService.java
├── LocalFileStorage.java
├── ObjectStorageFileStorage.java
└── config
    └── FileUploadProperties.java
```

### 5.3. Migration files

```text
V20260630_04__create_import_foundation.sql
VYYYYMMDD_NN__create_file_metadata.sql
```

### 5.4. Test resources

```text
src/test/resources/import-samples
├── lead-valid-small.csv
├── lead-invalid-phone.csv
├── lead-duplicate.csv
└── lead-invalid-header.csv
```

Không dùng file thật của khách hàng trong repo test.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Import batch table pattern

```sql
create table import_batches (
    import_batch_id uuid not null,
    tenant_id uuid not null,
    import_type varchar(50) not null,
    source_type varchar(50) not null,
    status varchar(50) not null,
    mapping_config jsonb,
    file_metadata_id uuid,
    original_file_name varchar(255),
    total_rows integer not null default 0,
    valid_rows integer not null default 0,
    error_rows integer not null default 0,
    duplicate_rows integer not null default 0,
    imported_at timestamptz,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_import_batches primary key (import_batch_id),
    constraint fk_import_batches__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_import_batches__status check (status in (
        'DRAFT','UPLOADED','VALIDATING','VALIDATED','PREVIEWED','CONFIRMED',
        'IMPORTING','COMPLETED','FAILED','CANCELLED','EXPIRED'
    )),
    constraint ck_import_batches__counts_non_negative check (
        total_rows >= 0
        and valid_rows >= 0
        and error_rows >= 0
        and duplicate_rows >= 0
    )
);

create index idx_import_batches__tenant_type_status_imported_at
on import_batches (tenant_id, import_type, status, imported_at desc);
```

Field rules:

| Field | Rule |
|---|---|
| `mapping_config` | Nullable trong `DRAFT`/`UPLOADED`; required từ `VALIDATING`/`PREVIEWED` trở đi nếu parser/preview được implement |
| `total_rows`, `valid_rows`, `error_rows`, `duplicate_rows` | Default `0`, check `>= 0` |
| `imported_at` | Nullable trong draft; chỉ set khi import/upload flow thật định nghĩa rõ meaning |
| `completed_at` | Nếu thêm sau này, chỉ set khi `COMPLETED`/`FAILED`/`CANCELLED`/`EXPIRED` theo decision |
| `file_metadata_id` | Nullable; chỉ có khi upload file thật |
| `original_file_name` | Nullable/display-only; không dùng làm object key |

### 6.2. Import row table pattern

```sql
create table import_rows (
    import_row_id uuid not null,
    tenant_id uuid not null,
    import_batch_id uuid not null,
    row_number integer not null,
    row_status varchar(50) not null,
    raw_row jsonb,
    normalized_row jsonb,
    error_details jsonb,
    created_at timestamptz not null default now(),
    constraint pk_import_rows primary key (import_row_id),
    constraint fk_import_rows__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_import_rows__import_batches foreign key (import_batch_id) references import_batches (import_batch_id),
    constraint ck_import_rows__status check (row_status in ('PENDING','VALID','ERROR','DUPLICATE','SKIPPED','IMPORTED')),
    constraint ck_import_rows__row_number_positive check (row_number > 0)
);

create index idx_import_rows__tenant_batch_status
on import_rows (tenant_id, import_batch_id, row_status);

create unique index ux_import_rows__batch_row_number
on import_rows (import_batch_id, row_number);
```

### 6.3. JSONB payload pattern

Mapping config:

```json
{
  "columns": {
    "phone": "Số điện thoại",
    "full_name": "Họ tên",
    "language_code": "Ngôn ngữ",
    "source": "Nguồn"
  }
}
```

Raw row:

```json
{
  "Số điện thoại": "0901234567",
  "Họ tên": "Nguyen Van A",
  "Ngôn ngữ": "English",
  "Nguồn": "Facebook"
}
```

Normalized row:

```json
{
  "phone": "0901234567",
  "full_name": "Nguyen Van A",
  "language_code": "EN",
  "source_type": "META_LEAD_ADS"
}
```

Error details:

```json
[
  {
    "field": "phone",
    "code": "INVALID_PHONE_NUMBER",
    "message": "Phone number format is invalid"
  }
]
```

Rules:

- `error_details` phải follow `ErrorDetail` schema của `06-exception-error-i18n-convention.md`: `field`, `code`, `message`.
- Không dùng `reason` trong import row error detail.
- Không lưu `raw_value` nếu chưa được approve và mask rõ.
- `raw_row` có thể chứa PII; API response phải mask/sanitize hoặc yêu cầu permission riêng.

### 6.4. File metadata table pattern

Chỉ cần nếu lưu file thật:

```sql
create table file_metadata (
    file_metadata_id uuid not null,
    tenant_id uuid not null,
    module varchar(100) not null,
    owner_type varchar(100) not null,
    owner_id uuid,
    original_file_name varchar(255) not null,
    object_key varchar(500) not null,
    content_type varchar(100) not null,
    file_size bigint not null,
    checksum_sha256 varchar(64),
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    constraint pk_file_metadata primary key (file_metadata_id),
    constraint fk_file_metadata__tenants foreign key (tenant_id) references tenants (tenant_id)
);

create index idx_file_metadata__tenant_owner
on file_metadata (tenant_id, owner_type, owner_id);
```

File metadata status:

```text
UPLOADED
AVAILABLE
DELETED
QUARANTINED
FAILED
```

Retention:

- Dev/QA: theo ops cleanup.
- Production/pilot: phải chốt PO/Ops/Security trước khi bật upload thật.
- Raw import file không giữ vô hạn mặc định.
- Delete/quarantine phải cập nhật `file_metadata.status`; không xóa metadata nếu còn cần audit/trace.

### 6.5. File storage abstraction pattern

```java
public interface FileStorage {

    void upload(String objectKey, InputStream inputStream, String contentType, long size);

    InputStream download(String objectKey);

    URI createDownloadUrl(String objectKey, Duration ttl);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
```

Implementations:

- `LocalFileStorage` for dev/test only.
- `ObjectStorageFileStorage` for S3-compatible/cloud storage when production upload is approved.

### 6.6. File validation pattern

Upload thật phải validate:

- File not empty.
- Max file size.
- Extension allowlist.
- Content-Type allowlist.
- Magic number/signature nếu format hỗ trợ.
- Filename sanitized.
- Tenant permission.
- Storage object key server-generated.
- Upload/download audit nếu file chứa dữ liệu khách hàng.

Example allowlist for lead import:

```text
.csv
.xlsx
```

Không trust `MultipartFile.getOriginalFilename()` để tạo stored path.

Production upload gate:

- Không bật public production upload nếu thiếu size/type/magic validation.
- Không bật nếu object storage policy/access control chưa chốt.
- Không bật nếu chưa có virus scan hoặc security risk acceptance.
- Không bật nếu retention policy chưa chốt.
- Không bật nếu access/audit policy chưa chốt.

### 6.7. Import status transition pattern

Allowed transition:

```text
DRAFT -> UPLOADED
UPLOADED -> VALIDATING
VALIDATING -> VALIDATED
VALIDATING -> FAILED
VALIDATED -> PREVIEWED
PREVIEWED -> CONFIRMED
CONFIRMED -> IMPORTING
IMPORTING -> COMPLETED
IMPORTING -> FAILED
DRAFT/UPLOADED/VALIDATED/PREVIEWED -> CANCELLED
```

Sprint 1 có thể implement subset nhưng không được tạo transition trái lifecycle.

Sprint 1 status subset:

| Status | Sprint 1 dùng? | Ghi chú |
|---|---|---|
| `DRAFT` | Có | Fixture/internal draft batch |
| `FAILED` | Có thể | Chỉ khi fixture/demo lỗi cần thể hiện |
| `EXPIRED` | Có thể defer | Chỉ bật nếu có cleanup job thật |
| `UPLOADED` | Reserved | Dành cho upload/parser thật |
| `VALIDATING` | Reserved | Dành cho parser/validation thật |
| `VALIDATED` | Reserved | Dành cho parser/validation thật |
| `PREVIEWED` | Reserved | Dành cho preview UX |
| `CONFIRMED` | Reserved | Dành cho confirm UX |
| `IMPORTING` | Reserved | Dành cho import thật |
| `COMPLETED` | Reserved | Dành cho import thật |

Fixture seed có thể dùng status reserved để demo history nếu được ghi rõ là fixture; không được claim parser/preview/confirm đã implement.

### 6.8. Tenant isolation pattern

Repository:

```java
Optional<ImportBatch> findByIdAndTenantId(UUID importBatchId, UUID tenantId);

Page<ImportRow> findByTenantIdAndImportBatchIdAndRowStatus(
        UUID tenantId,
        UUID importBatchId,
        ImportRowStatus rowStatus,
        Pageable pageable);
```

Direct batch/row access phải kiểm tenant.

### 6.9. Internal draft/fixture pattern

Nếu QA cần tạo data trước parser:

Default Sprint 1 - fixture command:

```text
mvn -Pqa seed-import-fixtures
```

Optional - protected internal API:

```text
POST /api/v1/internal/imports/leads/draft
```

Rules:

- Sprint 1 ưu tiên fixture command, không tạo internal draft API trừ khi FE demo thật sự cần.
- Internal draft API cần Tech Lead approval.
- Không bật public production.
- Disabled ngoài local/qa profile.
- Có permission/internal profile rõ.
- Document trong QA pack.
- Dữ liệu tenant-scoped.

## 7. QUY TẮC RIÊNG CỦA MAR IMPORT/FILE

### 7.1. Sprint 1 in scope

Sprint 1 được phép:

- Tạo import batch/row schema.
- Lưu `mapping_config`, `raw_row`, `normalized_row`, `error_details` bằng JSONB.
- Tạo repository/service foundation.
- Tạo API history/error nếu QA cần.
- Tạo fixture command nếu cần testability.
- Tạo internal API chỉ khi FE demo thật sự cần và đã được approve.
- Test tenant isolation và JSONB storage.

Sprint 1 không được claim parser/preview/confirm/import thật chỉ vì có fixture data hoặc status reserved.

### 7.2. Sprint 1 out of scope

Không claim đã hoàn chỉnh:

- Parser production.
- Preview/confirm UX.
- Dedup lead/customer/opportunity.
- Google Sheet connector.
- Meta Lead Ads connector.
- Virus scan production.
- Object storage production pipeline.

### 7.3. Parser rule future

Khi parser vào scope:

- Dùng thư viện ổn định cho CSV/Excel.
- Không parse CSV bằng split comma thủ công.
- Validate header.
- Validate row count/size.
- Normalize phone/email/source.
- Lưu row-level error.
- Không fail cả batch chỉ vì một row lỗi nếu product yêu cầu partial validation.

### 7.4. Storage decision

Sprint 1:

- Không cần lưu file thật nếu chỉ build foundation.
- Nếu demo cần upload, local storage chỉ dùng dev/QA và phải document.

Production upload future:

- Dùng object storage S3-compatible/cloud.
- DB chỉ lưu metadata/object key/checksum.
- Presigned URL TTL ngắn cho download.
- Virus scan hoặc security review bắt buộc trước public upload.
- Có retention policy cho raw file.
- Có access/audit policy cho download/export.

Không bật production upload nếu thiếu bất kỳ gate nào ở section 6.6.

### 7.5. Audit rule

Audit tối thiểu:

- `IMPORT_BATCH_CREATED` nếu API tạo batch.
- `IMPORT_BATCH_CONFIRMED` khi confirm được implement.
- `IMPORT_BATCH_COMPLETED` khi import thật được implement.
- `IMPORT_BATCH_CANCELLED` khi cancel được implement.

Không audit full raw row content.

### 7.6. Raw row API/export safety rule

`raw_row` có thể chứa PII:

- API response mặc định trả masked/sanitized raw row.
- Chỉ permission được approve mới xem raw row chi tiết.
- Error report export phải được audit bằng `DATA_EXPORTED` hoặc import export action đã chốt.
- Không trả raw file/raw row full payload cho user không có permission rõ.
- Không log raw row kể cả khi API trả masked response.

### 7.7. Status/cleanup scheduler rule

- `EXPIRED` chỉ dùng nếu có cleanup job/ticket thật theo `10-cache-async-scheduler-convention.md`.
- Nếu chưa có scheduler cleanup, giữ `EXPIRED` là reserved status.
- Không bật cleanup job chỉ để “đủ lifecycle”.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - import batch service

```java
@Transactional
public ImportBatchDetailResponse createDraft(CreateLeadImportDraftRequest request) {
    UUID tenantId = currentUserContext.currentTenantId();

    ImportBatch batch = ImportBatch.createDraft(
            tenantId,
            ImportType.LEAD,
            request.sourceType(),
            request.mappingConfig(),
            request.originalFileName()
    );

    ImportBatch saved = importBatchRepository.save(batch);

    auditService.record(AuditRecordCommand.builder()
            .tenantId(tenantId)
            .actorId(currentUserContext.currentActorId())
            .actorType(ActorType.USER)
            .actorRole(currentUserContext.currentUser().roleCode())
            .action(AuditAction.IMPORT_BATCH_CREATED)
            .resourceType(AuditResourceType.IMPORT_BATCH)
            .resourceId(saved.getId())
            .afterData(json.object("source_type", saved.getSourceType(), "status", saved.getStatus()))
            .requestId(LogContext.requestId())
            .build());

    return importBatchMapper.toDetailResponse(saved);
}
```

### 8.2. Good example - file storage path

```java
public String buildImportObjectKey(UUID tenantId, UUID importBatchId, String extension) {
    String safeExtension = extension.toLowerCase(Locale.ROOT);
    return "tenant/%s/import/%s/%s.%s".formatted(
            tenantId,
            importBatchId,
            UUID.randomUUID(),
            safeExtension
    );
}
```

### 8.3. Bad example - unsafe import

```java
String path = "/uploads/" + file.getOriginalFilename();
Files.write(Path.of(path), file.getBytes());
log.info("raw rows={}", rows);
```

Vấn đề:

- Trust client filename.
- Có nguy cơ path traversal.
- Load whole file into memory.
- Local app server storage không scale.
- Log raw rows có PII.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Claim import feature complete khi mới có foundation.
- Lưu file binary trong PostgreSQL.
- Lưu raw file trên application server làm production primary storage.
- Trust original filename để tạo path.
- Parse CSV bằng split comma thủ công.
- Bỏ tenant_id trong import tables/query.
- Log full raw row/raw file.
- Dùng JSONB để giấu stable relational data như catalog id nếu cần join.
- Tạo lead/customer/opportunity từ import foundation khi scope chưa approved.
- Public internal draft API ở production.
- Tạo internal draft API khi fixture command đã đủ testability.
- Claim reserved status đồng nghĩa parser/preview/confirm đã implement.
- Trả raw_row full PII qua API/export không permission/audit.
- Giữ raw import file vô hạn mặc định.
- Bỏ validation file size/content type.

## 10. TESTING CONVENTIONS

### 10.1. Import foundation test

Test:

- Create import batch.
- Store mapping config JSONB.
- Store raw row JSONB.
- Store normalized row JSONB.
- Store error details JSONB.
- Query error rows by tenant/batch/status.
- Status subset Sprint 1 không claim parser/preview/confirm thật.
- Counts không âm.
- `error_details` dùng `field`, `code`, `message`, không dùng `reason`.

### 10.2. Tenant isolation test

Test:

- Tenant A không xem batch Tenant B.
- Tenant A không xem row errors Tenant B.
- Direct `batch_id` khác tenant trả not found.
- Raw row/detail response tenant khác không leak.

### 10.3. File validation test

Nếu upload thật được implement:

- Empty file rejected.
- Oversized file rejected.
- Unsupported extension rejected.
- Content type mismatch rejected.
- Magic number mismatch rejected nếu format hỗ trợ.
- Path traversal filename sanitized/rejected.
- Object key không chứa original filename raw.
- Public upload bị chặn nếu gate security/retention/audit chưa đủ.

### 10.4. Storage integration test

Nếu dùng object storage:

- Upload success.
- Download/presigned URL generated.
- Delete success.
- Metadata persisted.
- Storage failure handled with error envelope.

### 10.5. Audit/import test

Test:

- Import batch created audit exists.
- Audit payload không chứa raw row full PII.
- Import error response follows error detail convention.
- Raw row API/export masking hoặc permission gate đúng.
- Error report export được audit nếu implement.

## 11. CODE REVIEW CHECKLIST

- [ ] Import tables tenant-scoped.
- [ ] Import query có tenant filter.
- [ ] JSONB dùng cho mapping/raw/normalized/error.
- [ ] Stable relational data không bị giấu không cần thiết trong JSONB.
- [ ] File binary không lưu trong DB.
- [ ] Object key server-generated.
- [ ] File validation có size/type/extension nếu upload.
- [ ] Production upload gate đã đủ validation/storage/virus-scan-or-acceptance/retention/audit.
- [ ] Không log raw row/file content.
- [ ] Fixture command là default Sprint 1; internal API nếu có phải local/QA-only và approved.
- [ ] Raw row API/export được mask/restrict theo permission và audit.
- [ ] Import status transition hợp lệ.
- [ ] Sprint 1 status subset không oversell parser/preview/confirm.
- [ ] Counts có check non-negative.
- [ ] `error_details` theo schema `field`, `code`, `message`.
- [ ] Audit import milestone nếu API tạo batch/confirm.
- [ ] Tests cover tenant isolation và JSONB behavior.
- [ ] Docs/demo không oversell parser/connector chưa làm.

## 12. TÀI LIỆU LIÊN QUAN

- `03-rest-api-convention.md` - Import API/error response.
- `04-database-flyway-convention.md` - JSONB/table/index/migration.
- `05-security-auth-authz-convention.md` - Import permissions.
- `08-audit-convention.md` - Import audit events.
- `09-testing-quality-convention.md` - Import tests.
- `10-cache-async-scheduler-convention.md` - Cleanup scheduler/async import events.
- OASIS reference: `fileupload_convention.md`, `database_convention.md`, `audit_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Làm rõ Sprint 1 import status subset, fixture command là default, field/count/nullability rules, `ErrorDetail` schema, raw row masking/export audit, file metadata retention và production upload security gate. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa import/file/storage convention theo pattern OASIS, map sang lead import foundation của MAR Sprint 1. |
