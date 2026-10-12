package vn.cmc.coursemanagement.config;

import vn.cmc.coursemanagement.controller.CourseForm;
import vn.cmc.coursemanagement.entity.CourseCategory;
import vn.cmc.coursemanagement.repository.CourseCategoryRepository;
import vn.cmc.coursemanagement.repository.CourseRepository;
import vn.cmc.coursemanagement.service.CatalogService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;

@Component
@Profile("!test")
public class DemoData implements CommandLineRunner {
    private final CatalogService catalog;
    private final CourseCategoryRepository categories;
    private final CourseRepository courses;
    private final JdbcTemplate jdbc;
    public DemoData(CatalogService catalog, CourseCategoryRepository categories, CourseRepository courses, JdbcTemplate jdbc) {
        this.catalog = catalog; this.categories = categories; this.courses = courses; this.jdbc = jdbc;
    }
    @Override
    @Transactional
    public void run(String... args) {
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_course_thumbnail ON \"CourseImages\" (\"CourseId\") WHERE \"IsThumbnail\" = true");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_course_name_ignore_case ON \"Courses\" (\"CourseCategoryId\", lower(\"Name\"))");
        // Chỉ tạo mẫu khi cả hai bảng còn trống; giữ nguyên dữ liệu đã sửa hoặc xóa ở lần chạy sau.
        if (categories.count() != 0 || courses.count() != 0) return;
        var web = category("Lập trình Web", "Xây dựng website và giao diện web.");
        var backend = category("Lập trình Backend", "API, Java và cơ sở dữ liệu.");
        var data = category("Khoa học dữ liệu", "Phân tích dữ liệu và học máy.");
        seed("ASP.NET Core", web, "2500000", 30, true, 2);
        seed("ReactJS", web, "1800000", 24, false, 1);
        seed("Java Spring Boot", backend, "3500000", 48, true, 3);
        seed("PostgreSQL", backend, "1200000", 20, true, 0);
        seed("Machine Learning cơ bản", data, "4200000", 60, false, 2);
    }
    private Long category(String name, String description) {
        CourseCategory category = new CourseCategory();
        category.setName(name); category.setDescription(description);
        return catalog.saveCategory(null, category).getId();
    }
    private void seed(String name, Long categoryId, String fee, int hours, boolean active, int imageCount) {
        CourseForm form = new CourseForm();
        form.setName(name); form.setCategoryId(categoryId); form.setTuitionFee(new BigDecimal(fee));
        form.setDuration(hours); form.setIsActive(active);
        form.setDescription("Khóa " + name + " gồm kiến thức nền tảng, bài thực hành và mini project.");
        var course = catalog.saveCourse(null, form);
        for (int i = 1; i <= imageCount; i++)
            catalog.addImage(course.getId(), "https://picsum.photos/seed/course-" + course.getId() + "-" + i + "/800/500");
    }
}
