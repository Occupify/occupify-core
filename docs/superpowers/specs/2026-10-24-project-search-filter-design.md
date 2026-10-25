# Design Document: Project Search & Filter with Full-Text Search (FTS)

## 1. Intent & Context
Occupify là sàn giao dịch việc làm tự do (freelance marketplace) hướng tới thị trường người dùng Việt Nam. Dịch vụ `occupify-core` chịu trách nhiệm quản lý dự án (`projects`), vị trí tuyển dụng (`job`) và kỹ năng (`skills`).

Tính năng này cung cấp API tìm kiếm toàn văn (Full-Text Search) và lọc đa tiêu chí cho dự án, giúp Freelancer nhanh chóng tiếp cận các cơ hội việc làm phù hợp với từ khóa, kỹ năng và mức ngân sách kỳ vọng.

---

## 2. Goals, Non-Goals & Anti-Goals

### 2.1. Goals
1. **Tìm kiếm toàn văn (Full-Text Search - FTS)**:
   - Hỗ trợ tiếng Việt linh hoạt: tìm kiếm có dấu và không dấu đều ra kết quả (ví dụ "lập trình" và "lap trinh").
   - Tìm kiếm từ khóa trên:
     - Tiêu đề dự án (`projects.title`)
     - Mô tả chi tiết dự án (`projects.description`)
     - Tiêu đề vị trí tuyển dụng con đang mở (`job.title` với `job.status = 'RECRUITING'`)
     - Mô tả vị trí tuyển dụng con đang mở (`job.description` với `job.status = 'RECRUITING'`)
     - Tên các kỹ năng yêu cầu của vị trí đang mở (`skills.name`)
2. **Bộ lọc đa tiêu chí (Filtering)**:
   - **Khoảng ngân sách (`minPrice` - `maxPrice`)**: Dự án được giữ lại nếu có ít nhất một vị trí công việc đang tuyển (`status = RECRUITING`) có ngân sách giao thoa (overlap) với khoảng lọc:
     `(job.budget_max >= minPrice OR minPrice IS NULL) AND (job.budget_min <= maxPrice OR maxPrice IS NULL)`. Nếu job chỉ có giá trị `budget_min` hoặc `budget_max`, so sánh tương ứng.
   - **Kỹ năng (`skills`)**: Lọc theo danh sách kỹ năng theo cơ chế `OR` (chỉ cần dự án có ít nhất một vị trí đang tuyển yêu cầu một trong các kỹ năng đã chọn).
   - **Điều kiện hiển thị mặc định cho Freelancer**: Chỉ hiển thị các dự án đang mở (`projects.status = OPEN`) và có ít nhất 1 vị trí công việc đang tuyển (`job.status = RECRUITING`). Bất kỳ dự án nào đã đóng (`CLOSED`) hoặc không còn vị trí nào tuyển (`FILLED`/`CLOSED`) đều bị loại bỏ khỏi kết quả tìm kiếm.
   - **Khả năng tương thích ngược**: Tiếp tục hỗ trợ tham số `employerId` và `status` tùy chọn trong request để phục vụ các màn hình quản lý dự án của chính nhà tuyển dụng nếu có truyền vào.
3. **Sắp xếp mặc định (Sorting)**:
   - Mặc định sắp xếp theo ngày đăng mới nhất (`projects.createdAt DESC`).
4. **Phân trang (Pagination)**:
   - Hỗ trợ phân trang chuẩn theo `Pageable` (`page`, `size`), trả về đầy đủ metadata phân trang (`page`, `size`, `totalElements`, `totalPages`).
5. **Cấu trúc dữ liệu Card trả về (Phương án A - Project-Level Card)**:
   - Thông tin nhà tuyển dụng: Avatar (`employer.avatarUrl`), Họ tên (`employer.fullName`).
   - Thông tin dự án: ID, Tiêu đề (`title`), Mô tả ngắn (`description`), Thời điểm đăng (`createdAt`), Chu kỳ lương (`period`).
   - Dải ngân sách tổng hợp: Mức tối thiểu và tối đa được gộp từ các vị trí công việc đang mở (`salaryMin` - `salaryMax`), hoặc `startPrice` của dự án.
   - Danh sách kỹ năng tổng hợp: Toàn bộ các tag kỹ năng từ các vị trí đang tuyển của dự án.
   - Tóm tắt số lượng và tên các vị trí đang mở nhận thầu (`openJobsCount`, `jobsSummary`).

### 2.2. Non-Goals (Không thuộc phạm vi Core Service)
- **Lưu lịch sử tìm kiếm gần đây**: Không lưu trên database backend của Core (do phía Frontend/Client tự quản lý trên LocalStorage).
- **Lưu công việc yêu thích (Bookmarks)**: Không thực hiện trong task này (được tách thành tính năng riêng hoặc lưu tại Identity/Client).

### 2.3. Anti-Goals
- Không trả về các dự án đã đóng hoặc các dự án chỉ có các vị trí đã tuyển đủ nhân sự trong luồng tìm kiếm công việc.
- Không áp dụng logic `AND` cho bộ lọc kỹ năng (tránh trường hợp kết quả bị rỗng do đòi hỏi dự án phải có đồng thời tất cả các kỹ năng được chọn).
- Không để FTS bị phân biệt đối xử giữa tiếng Việt có dấu và không dấu (gây trải nghiệm kém cho người dùng di động khi gõ nhanh).
- Không so khớp từ khóa hoặc kỹ năng với các vị trí đã đóng (`FILLED`/`CLOSED`) nhằm tránh hiển thị kết quả gây hiểu lầm cho người tìm việc.

---

## 3. Architectural & Technical Decisions

### 3.1. PostgreSQL Full-Text Search với `unaccent`
- **Extension**: Kích hoạt extension `unaccent` trong PostgreSQL để chuẩn hóa văn bản tiếng Việt về dạng không dấu.
- **Search Dictionary**: Sử dụng từ điển `simple` kết hợp hàm `unaccent()` để bóc tách token và so khớp `tsvector` @@ `plainto_tsquery('simple', unaccent(:keyword))`.
- **GIN Indexing**: Tạo chỉ mục GIN trên biểu thức `to_tsvector('simple', unaccent(coalesce(title, '') || ' ' || coalesce(description, '')))` cho bảng `projects` để tăng tốc độ truy vấn FTS.

### 3.2. Query Strategy: Dynamic Specification hoặc Native SQL
- Do yêu cầu tìm kiếm liên kết xuyên suốt `projects` -> `job` (chỉ các job `RECRUITING`) -> `job_skills` -> `skills` cùng với hàm PostgreSQL FTS (`unaccent`, `to_tsvector`), builder có thể lựa chọn:
  - Sử dụng **JPA Specification** với custom expressions gọi hàm `unaccent`, HOẶC:
  - Sử dụng **Native SQL Query** với phân trang qua `@Query` trong `ProjectRepository` kết hợp đếm `countQuery` để đảm bảo hiệu năng và tận dụng 100% cú pháp FTS của PostgreSQL.

### 3.3. DTO & API Contract
- **Endpoint**: Tích hợp trực tiếp vào endpoint danh sách dự án `GET /api/v1/projects`.
- **Request Parameters (`ProjectFilterRequest`)**:
  - `keyword` (`String` - optional): Từ khóa tìm kiếm FTS.
  - `skills` (`List<String>` - optional): Danh sách tên kỹ năng cần lọc (logic `OR`).
  - `minPrice` (`BigDecimal` - optional): Mức giá sàn tối thiểu.
  - `maxPrice` (`BigDecimal` - optional): Mức giá trần tối đa.
  - `period` (`ProjectPeriod` - optional): Chu kỳ thanh toán (DAY, FIXED, HOURLY, WEEKLY, MONTHLY).
  - `experienceLevel` (`ExperienceLevel` - optional): Cấp độ kinh nghiệm.
  - `employerId` (`UUID` - optional): Lọc theo nhà tuyển dụng cụ thể (cho trang cá nhân/dashboard).
  - `status` (`ProjectStatus` - optional): Trạng thái dự án (mặc định cho tìm kiếm công việc là `OPEN`).
  - `page`, `size`, `sort` (`Pageable`): Tham số phân trang, mặc định `sort=createdAt,desc`.
- **Response Format**:
  - `PageResponse<ProjectResponse>` trong đó `ProjectResponse` cung cấp đầy đủ các trường phục vụ hiển thị Card Phương án A (bao gồm `employerSummary`, danh sách `skills`, `salaryMin`, `salaryMax` / `startPrice`, `period`, `status`, `createdAt`, `openJobsCount`).

---

## 4. Builder's Implementation Calls (Đã chốt)
1. **Budget Overlap**: Ngân sách được tính theo quy tắc dải giao thoa `(budget_max >= minPrice OR minPrice IS NULL) AND (budget_min <= maxPrice OR maxPrice IS NULL)`.
2. **Active Jobs Scope**: Chỉ các vị trí `job.status = 'RECRUITING'` mới được đưa vào tính toán FTS matching, filter skills và tính dải lương.
3. **Default Sorting**: Luôn áp dụng sắp xếp theo ngày tạo mới nhất `projects.createdAt DESC` làm mặc định.
4. **Backward Compatibility**: Giữ nguyên tính tương thích của `ProjectFilterRequest` để các màn hình quản trị của Employer vẫn có thể lọc theo `employerId` khi cần.
