# Mini Project Quản lý khóa học

Java 21 · Spring Boot 3.5.16 · Gradle Wrapper 8.14.3 · Spring MVC · JPA/Hibernate · PostgreSQL · Thymeleaf · Jakarta Validation.

## Chạy trên Windows

Máy hiện có JDK 21 tại `C:\Program Files\Java\jdk-21.0.11`, nhưng Java mặc định trong PATH là 11. Mở PowerShell tại Lesson7:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Mật khẩu PostgreSQL' -AsSecureString)).Password
.\gradlew.bat --version
.\gradlew.bat build
.\gradlew.bat bootRun
```

Truy cập http://localhost:8080/. Dừng ứng dụng bằng Ctrl+C. Có thể chạy JAR sau build:

```powershell
java -jar build\libs\course-management-1.0.0.jar
```

Lệnh nhập mật khẩu không ghi mật khẩu vào file hoặc nội dung lịch sử lệnh. Biến chỉ có hiệu lực trong tiến trình PowerShell và các tiến trình con. Để cấu hình lâu dài: Windows Settings → tìm “Edit environment variables for your account” → New → tên `DB_PASSWORD`, giá trị là mật khẩu PostgreSQL của bạn. Khởi động lại terminal/IDE để nhận biến mới. Không đưa giá trị thật vào Git.

## Thiết lập PostgreSQL

Thông tin mặc định: localhost:5432, user `postgres`, database `course_management_db`.

Trong pgAdmin, kết nối server bằng mật khẩu PostgreSQL của bạn. Query Tool trên database `postgres`:

```sql
SELECT datname FROM pg_database WHERE datname = 'course_management_db';
```

Chỉ khi chưa có database, chạy riêng lệnh sau (ngoài transaction):

```sql
CREATE DATABASE course_management_db;
```

Ứng dụng dùng `spring.jpa.hibernate.ddl-auto=update`: tạo/cập nhật schema thực hành, không drop database hoặc bảng hiện có. Mật khẩu lấy từ `${DB_PASSWORD}` trong `application.properties`. Không đổi cấu hình này thành `create`/`create-drop` khi dùng database thật.

Tên bảng/cột dùng PascalCase và được quote nhất quán. Sau khi ứng dụng chạy, mở Query Tool của `course_management_db`:

```sql
SELECT * FROM "CourseCategories";
SELECT * FROM "Courses";
SELECT * FROM "CourseImages";
SELECT "CourseId", COUNT(*)
FROM "CourseImages"
WHERE "IsThumbnail" = true
GROUP BY "CourseId"
HAVING COUNT(*) > 1;
```

Query cuối phải không có dòng nào.

Hai unique index PostgreSQL bổ sung:
- `uk_course_name_ignore_case`: không trùng tên, không phân biệt hoa/thường, trong cùng category.
- `uk_course_thumbnail`: mỗi Course tối đa một ảnh có IsThumbnail=true.

Nếu kết nối thất bại:
- `no password supplied` hoặc chưa cấu hình `DB_PASSWORD`: cấu hình biến rồi chạy lại.
- `password authentication failed`: kiểm tra mật khẩu của user postgres.
- `database ... does not exist`: tạo database như trên.
- `connection refused`: kiểm tra PostgreSQL service, host và port.
- Nếu database cũ đã có tên trùng hoặc nhiều thumbnail, unique index sẽ không tạo được. Kiểm tra và sửa dữ liệu thủ công; ứng dụng không tự xóa dữ liệu để vượt lỗi.

**Tình trạng máy khi triển khai:** PostgreSQL 18 đang chạy và port 5432 mở; phiên hiện tại chưa có DB_PASSWORD. Lệnh psql không tương tác xác nhận lỗi `fe_sendauth: no password supplied`. Chưa xác minh database đã tồn tại hoặc startup với PostgreSQL thật.

## IntelliJ / Rider

**IntelliJ:** mở thư mục Lesson7 dưới dạng Gradle project. Chọn Project SDK 21 và Gradle JVM 21. Tạo/chọn Run Configuration cho `CourseManagementApplication` → Environment variables → thêm `DB_PASSWORD`. Nếu chạy Gradle bootRun từ Run Configuration, đặt biến ở cấu hình Gradle tương ứng. Không chia sẻ Run Configuration chứa mật khẩu lên Git.

**Rider:** dùng Terminal tích hợp, chạy các lệnh PowerShell ở trên. Nếu cấu hình Java/Gradle khả dụng trong bản IDE/plugin đang dùng, đặt Environment variables = `DB_PASSWORD` trong cấu hình đó. Rider không mặc định là IDE Java; không cần thay đổi project để chạy bằng terminal. Sau khi đặt biến Windows, mở lại IDE.

## Cấu trúc và file chính

```text
Lesson7/
├── build.gradle, settings.gradle, gradlew, gradlew.bat
├── gradle/wrapper/
├── GLOSSARY.md
├── src/main/java/vn/cmc/coursemanagement/
│   ├── CourseManagementApplication.java
│   ├── entity/       CourseCategory, Course, CourseImage
│   ├── repository/   3 repository, CourseFilter
│   ├── service/      CatalogService, BusinessException, NotFoundException
│   ├── controller/   Home, Course, Category, GlobalExceptionHandler
│   │                CourseForm, ImageForm
│   └── config/       DemoData, WebSecurityConfig
├── src/main/resources/
│   ├── application.properties, messages.properties
│   ├── templates/    index, fragments, courses, categories, error
│   └── static/       css/style.css, js/images.js
├── src/test/         CatalogIntegrationTest, application-test.properties
└── scripts/          browser_smoke.py
```

CourseForm và ImageForm chỉ chứa dữ liệu được phép nhập; không có mapper/interface service bổ sung. Category form chỉ cho binding name/description. Spring Security giữ CSRF cho form POST và cho phép truy cập tất cả trang; không thêm đăng nhập/JWT.

## Entity và business rule

| Entity | Bảng | Quan hệ |
|---|---|---|
| CourseCategory | CourseCategories | OneToMany Course; không cascade xóa Course |
| Course | Courses | ManyToOne Category qua CourseCategoryId; OneToMany CourseImage |
| CourseImage | CourseImages | ManyToOne Course qua CourseId |

Xóa Course cascade xóa ảnh. Xóa Category bị chặn nếu còn Course. Service khóa category khi kiểm tra và lưu tên; tên được trim và kiểm tra không phân biệt hoa/thường. Hai lần cập nhật thumbnail nằm trong cùng transaction và khóa Course. ImageId được tra cùng CourseId để không đổi ảnh của khóa khác.

Specification đưa điều kiện tên, category, trạng thái và khoảng học phí xuống database; Sort cũng chạy ở database. Không lọc danh sách Course bằng Java Stream. Ngắn hạn/Dài hạn được tính từ duration khi hiển thị, không lưu cột riêng.

URL ảnh chỉ chấp nhận HTTP/HTTPS, host hợp lệ, không có thông tin user/password. Server không tải URL ảnh; trình duyệt tải ảnh và hiển thị fallback nếu lỗi.

## Dữ liệu mẫu

Chỉ khởi tạo khi cả bảng category và course còn trống. Không tự chèn lại những khóa người dùng đã sửa/xóa khi chạy lần sau.

| Loại | Khóa học | Học phí | Giờ | Tuyển sinh | Số ảnh |
|---|---|---:|---:|---|---:|
| Lập trình Web | ASP.NET Core | 2.500.000 | 30 | Đang | 2 |
| Lập trình Web | ReactJS | 1.800.000 | 24 | Ngừng | 1 |
| Lập trình Backend | Java Spring Boot | 3.500.000 | 48 | Đang | 3 |
| Lập trình Backend | PostgreSQL | 1.200.000 | 20 | Đang | 0 |
| Khoa học dữ liệu | Machine Learning cơ bản | 4.200.000 | 60 | Ngừng | 2 |

Ảnh mẫu dùng URL picsum.photos. Khả năng tải thực tế phụ thuộc kết nối internet và dịch vụ ngoài.

## Skill thực sự sử dụng

Đã đọc và áp dụng: `mattpocock-skills:domain-modeling`, `mattpocock-skills:codebase-design`, `spring-boot-backend`, `postgresql-backend`, `frontend-design`, `webapp-testing`, `mattpocock-skills:diagnosing-bugs` (sửa cách chạy browser test trên Windows), `ponytail:ponytail` và `ponytail:ponytail-review`.

## Kiểm thử và giới hạn

- Gradle Wrapper 8.14.3 chạy bằng JDK 21, distribution có SHA-256.
- `gradlew build`: thành công; 21 test tích hợp, 0 lỗi/0 thất bại/0 bỏ qua.
- Test chạy Spring Boot, JPA và Thymeleaf bằng H2 trong bộ nhớ, không truy cập PostgreSQL hiện có.
- Đã kiểm tra mapping, CRUD, validation Create/Edit, duplicate name, ID sai, khoảng phí sai, lọc kết hợp, ba kiểu sort, ngắn/dài hạn, URL ảnh, thumbnail, ảnh khác Course, cascade xóa và chặn xóa category có Course.
- Test dữ liệu mẫu dùng service/JPA H2 thật; riêng JdbcTemplate tạo hai PostgreSQL index được mock vì H2 không hỗ trợ partial index đó.
- Review code theo quy mô bài tập/demo một người dùng: đã rà soát trust boundary, transaction, CSRF, quyền sở hữu ảnh, query và cascade.
- PostgreSQL startup, hai index trên PostgreSQL và tranh chấp đồng thời trên PostgreSQL chưa được kiểm chứng. Không coi test H2 là bằng chứng PostgreSQL đã chạy.
- Edge headless/Playwright: 10 nhóm kiểm tra đạt, mã thoát 0; đã xem ảnh chụp 1440px và 390px. Kết quả tại `build/browser-results/results.txt`, ảnh chụp cùng thư mục. Browser dùng H2 riêng; lỗi tải ảnh ngoài được mô phỏng bằng chặn request HTTPS. Học phí thập phân 0,25 ₫ được kiểm tra riêng trên cả List và Detail bằng MockMvc.

## Checklist 14 nhóm yêu cầu triển khai

| Nhóm đề | Trạng thái và phần triển khai |
|---|---|
| 1. Entity | Đủ 3 entity, trường dữ liệu, mapping và khóa ngoại |
| 2. Validation | Name bắt buộc; fee/duration >0; category tồn tại; không trùng tên cùng category; Create/Edit |
| 3. Category CRUD | List/Create/Edit/Delete; chặn xóa khi có Course |
| 4. Course CRUD | List/Detail/Create/Edit/Delete; dropdown category từ DB |
| 5. Search/Filter/Sort | Kết hợp mọi bộ lọc ở DB; giữ giá trị; báo khoảng phí sai và không kết quả |
| 6. Thời lượng | <=30 ngắn hạn, >30 dài hạn; không thêm cột |
| 7. Hình ảnh | Nhiều URL; Detail; chọn/đổi thumbnail trong transaction; kiểm tra ảnh thuộc Course; fallback |
| 8. Trạng thái | Hiển thị cả hai trạng thái; lọc true/false |
| 9. Xử lý lỗi | ID Course/Category/Image sai, validation, tên trùng, category đang dùng; trang lỗi thân thiện |
| 10. PostgreSQL | localhost:5432/course_management_db; postgres; DB_PASSWORD; ddl-auto=update; chờ xác minh DB thật |
| 11. Dữ liệu mẫu | 3 Category, 5 Course, hai nhóm thời lượng/trạng thái, 8 ảnh; không chèn trùng |
| 12. Giao diện | Thymeleaf tiếng Việt; navbar; đầy đủ trang, bảng và thao tác; CSS cục bộ |
| 13. Cấu trúc | Controller/Entity/Repository/Service/Config, resources theo đề |
| 14. Kiểm tra | Wrapper/build, 21 test và 10 nhóm browser đạt; PostgreSQL chưa xác minh |

## Checklist demo 14 bước

Các ID lấy từ danh sách thực tế, không giả định luôn là 1/2/3.

| Bước | URL hoặc thao tác | Kết quả mong đợi |
|---:|---|---|
| 1 | pgAdmin → course_management_db → Tables; chạy ba SELECT phía trên | Có CourseCategories, Courses, CourseImages và dữ liệu |
| 2 | pgAdmin → Constraints của Courses/CourseImages | CourseCategoryId và CourseId là khóa ngoại; mỗi khóa/ảnh thuộc một cha |
| 3 | /courses và /categories qua navbar | Có danh sách khóa học và loại khóa học |
| 4 | /courses → nhập tên “Spring” → Áp dụng bộ lọc | Chỉ khóa có một phần tên phù hợp; giữ tên nhập |
| 5 | Chọn loại “Lập trình Web” | Chỉ khóa thuộc loại đó |
| 6 | Chọn “Đang tuyển sinh”, sau đó “Ngừng tuyển sinh” | Chỉ khóa có trạng thái tương ứng; khi chọn tất cả vẫn có khóa ngừng |
| 7 | Nhập min=1000000, max=2500000; sau đó min=5000000,max=1000000 | Lần đầu lọc đúng; lần sau báo min không được lớn hơn max |
| 8 | Chọn học phí tăng, giảm, rồi thời lượng tăng | Các dòng lần lượt sắp xếp theo lựa chọn tại DB |
| 9 | Xóa bộ lọc, xem cột thời lượng/phân loại | <=30 giờ Ngắn hạn; >30 giờ Dài hạn |
| 10 | /courses/new hoặc /courses/{id}/edit → lưu hợp lệ, thử tên trùng/fee=0/duration=0 | Lưu hợp lệ; dữ liệu sai bị chặn và có lỗi trên form |
| 11 | /courses/{id} → thêm URL https://example.com/course.jpg; thử ftp://example.com/a.jpg | URL HTTP/HTTPS được thêm; URL sai bị chặn; ảnh hỏng có fallback |
| 12 | Detail có ít nhất 2 ảnh → “Đặt làm ảnh đại diện” | Ảnh mới là thumbnail, ảnh cũ trở thành thường; chỉ một thumbnail |
| 13 | /categories → Xóa loại đang có Course → xác nhận | “Không thể xóa loại khóa học vì đang có khóa học thuộc loại này.” |
| 14 | /courses/999999999 | HTTP 404, “Không tìm thấy khóa học.” và nút về danh sách |

Không tự commit/push. Không thêm React, upload file, Docker hoặc chức năng ngoài đề.
