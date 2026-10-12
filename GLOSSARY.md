# Quản lý khóa học
Danh mục các khóa học và thông tin tuyển sinh phục vụ quản lý đào tạo.

## Language
**Loại khóa học (CourseCategory)**:
Nhóm chuyên môn chứa các khóa học.
_Avoid_: lớp học

**Khóa học (Course)**:
Chương trình đào tạo có học phí và thời lượng, thuộc đúng một loại khóa học.
_Avoid_: buổi học

**Hình ảnh khóa học (CourseImage)**:
Hình ảnh được tham chiếu bằng URL và thuộc đúng một khóa học.
_Avoid_: tệp upload

**Ảnh đại diện (Thumbnail)**:
Ảnh được chọn để đại diện cho khóa học; mỗi khóa học có tối đa một ảnh đại diện.
_Avoid_: mọi hình ảnh

**Đang tuyển sinh (IsActive)**:
Trạng thái đang nhận người học, không phải trạng thái xóa khóa học.
_Avoid_: còn tồn tại

**Ngắn hạn / Dài hạn**:
Khóa học tối đa 30 giờ là ngắn hạn; trên 30 giờ là dài hạn.
_Avoid_: số ngày học
