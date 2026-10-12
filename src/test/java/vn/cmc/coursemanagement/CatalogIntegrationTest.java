package vn.cmc.coursemanagement;

import vn.cmc.coursemanagement.controller.CourseForm;
import vn.cmc.coursemanagement.entity.*;
import vn.cmc.coursemanagement.repository.*;
import vn.cmc.coursemanagement.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogIntegrationTest {
    @Autowired CatalogService catalog;
    @Autowired CourseRepository courses;
    @Autowired CourseImageRepository images;
    @Autowired CourseCategoryRepository categories;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    Long web, backend;
    Course spring, react, sql;
    @BeforeEach void setup() {
        web = category("Web").getId(); backend = category("Backend").getId();
        spring = catalog.saveCourse(null, form("Spring Boot", web, "3000000", 48, true));
        react = catalog.saveCourse(null, form("ReactJS", web, "1800000", 30, false));
        sql = catalog.saveCourse(null, form("PostgreSQL", backend, "1200000", 20, true));
    }
    private CourseCategory category(String name) {
        CourseCategory c = new CourseCategory(); c.setName(name); return catalog.saveCategory(null, c);
    }
    private CourseForm form(String name, Long category, String fee, int duration, boolean active) {
        CourseForm f = new CourseForm(); f.setName(name); f.setCategoryId(category);
        f.setTuitionFee(new BigDecimal(fee)); f.setDuration(duration); f.setIsActive(active);
        f.setDescription("Mô tả kiểm thử"); return f;
    }
    private CourseFilter filter() { return new CourseFilter(); }

    @Test void demoDataIsCompleteAndDoesNotDuplicate() {
        courses.deleteAll(); courses.flush(); categories.deleteAll(); categories.flush(); em.clear();
        var demo = new vn.cmc.coursemanagement.config.DemoData(catalog, categories, courses,
            org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class));
        demo.run();
        assertThat(categories.count()).isEqualTo(3);
        assertThat(courses.count()).isEqualTo(5);
        assertThat(images.count()).isEqualTo(8);
        for (Course c : catalog.search(filter()))
            assertThat(c.getImages()).filteredOn(CourseImage::getIsThumbnail).hasSize(c.getImages().isEmpty() ? 0 : 1);
        demo.run();
        assertThat(courses.count()).isEqualTo(5);
        assertThat(images.count()).isEqualTo(8);
    }
    @Test void tuitionFeeKeepsDecimalPartOnListAndDetail() throws Exception {
        catalog.saveCourse(spring.getId(), form("Spring Boot", web, "0.25", 48, true));
        mvc.perform(get("/courses")).andExpect(content().string(org.hamcrest.Matchers.containsString("0,25 ₫")));
        mvc.perform(get("/courses/" + spring.getId())).andExpect(content().string(org.hamcrest.Matchers.containsString("0,25 ₫")));
    }
    @Test void mappingsAndDurationBoundary() {
        em.flush(); em.clear();
        Course loaded = catalog.course(react.getId());
        assertThat(loaded.getCategory().getId()).isEqualTo(web);
        assertThat(loaded.getDurationLabel()).isEqualTo("Ngắn hạn");
        assertThat(catalog.course(spring.getId()).getDurationLabel()).isEqualTo("Dài hạn");
        assertThat(categories.findById(web).orElseThrow().getCourses()).hasSize(2);
    }
    @Test void combinesAllFiltersInDatabase() {
        CourseFilter f = filter(); f.setName("spring"); f.setCategoryId(web); f.setIsActive(true);
        f.setMinFee(new BigDecimal("2000000")); f.setMaxFee(new BigDecimal("3500000")); f.setSort("feeDesc");
        assertThat(catalog.search(f)).extracting(Course::getId).containsExactly(spring.getId());
        f.setIsActive(false); assertThat(catalog.search(f)).isEmpty();
    }
    @Test void feeAndDurationSortsAndInactiveCourses() {
        CourseFilter f = filter(); f.setSort("feeAsc");
        assertThat(catalog.search(f)).extracting(Course::getId).containsExactly(sql.getId(), react.getId(), spring.getId());
        f.setSort("feeDesc");
        assertThat(catalog.search(f)).extracting(Course::getId).containsExactly(spring.getId(), react.getId(), sql.getId());
        f.setSort("durationAsc");
        assertThat(catalog.search(f)).extracting(Course::getDuration).containsExactly(20, 30, 48);
        f.setIsActive(false); assertThat(catalog.search(f)).extracting(Course::getId).containsExactly(react.getId());
    }
    @Test void literalSearchDoesNotTreatPercentAsWildcard() {
        catalog.saveCourse(null, form("100% thực hành", web, "100", 1, true));
        CourseFilter f = filter(); f.setName("%");
        assertThat(catalog.search(f)).extracting(Course::getName).containsExactly("100% thực hành");
    }
    @Test void duplicateNameInSameCategoryIsRejected() {
        assertThatThrownBy(() -> catalog.saveCourse(null, form(" spring boot ", web, "100", 1, true)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("đã tồn tại");
    }
    @Test void sameNameInDifferentCategoryAndEditingSelfAreAllowed() {
        catalog.saveCourse(null, form("Spring Boot", backend, "100", 1, true));
        catalog.saveCourse(spring.getId(), form("Spring Boot", web, "3100000", 49, false));
        assertThat(catalog.course(spring.getId()).getTuitionFee()).isEqualByComparingTo("3100000");
    }
    @Test void duplicateEditIsRejected() {
        assertThatThrownBy(() -> catalog.saveCourse(react.getId(), form("Spring Boot", web, "100", 1, true)))
            .isInstanceOf(BusinessException.class);
    }
    @Test void missingCategoryIsRejected() {
        assertThatThrownBy(() -> catalog.saveCourse(null, form("Missing", 999999L, "100", 1, true)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("không tồn tại");
    }
    @Test void cannotDeleteUsedCategory() {
        assertThatThrownBy(() -> catalog.deleteCategory(web)).isInstanceOf(BusinessException.class)
            .hasMessage("Không thể xóa loại khóa học vì đang có khóa học thuộc loại này.");
    }
    @Test void deleteUnusedCategoryAndCourseWithImages() {
        Long empty = category("Empty").getId(); catalog.deleteCategory(empty);
        assertThat(categories.existsById(empty)).isFalse();
        catalog.addImage(sql.getId(), "https://example.com/one.jpg");
        Long imageId = catalog.course(sql.getId()).getImages().getFirst().getId();
        catalog.deleteCourse(sql.getId());
        assertThat(courses.existsById(sql.getId())).isFalse(); assertThat(images.existsById(imageId)).isFalse();
    }
    @Test void exactlyOneThumbnailAfterAddingAndChanging() {
        catalog.addImage(spring.getId(), "https://example.com/one.jpg");
        catalog.addImage(spring.getId(), "https://example.com/two.jpg");
        em.flush(); em.clear();
        Course c = catalog.course(spring.getId()); List<CourseImage> list = c.getImages();
        assertThat(list).hasSize(2);
        assertThat(list).filteredOn(CourseImage::getIsThumbnail).hasSize(1);
        Long second = list.get(1).getId(); catalog.changeThumbnail(c.getId(), second);
        em.flush(); em.clear();
        assertThat(catalog.course(c.getId()).getThumbnail().getId()).isEqualTo(second);
        assertThat(catalog.course(c.getId()).getImages()).filteredOn(CourseImage::getIsThumbnail).hasSize(1);
    }
    @Test void cannotUseImageFromAnotherCourse() {
        catalog.addImage(sql.getId(), "https://example.com/sql.jpg");
        Long imageId = catalog.course(sql.getId()).getImages().getFirst().getId();
        assertThatThrownBy(() -> catalog.changeThumbnail(spring.getId(), imageId)).isInstanceOf(NotFoundException.class);
    }
    @Test void imageUrlBoundary() {
        assertThat(CatalogService.isHttpUrl("https://example.com/a.jpg?size=2")).isTrue();
        assertThat(CatalogService.isHttpUrl("HTTP://example.com/a.jpg")).isTrue();
        for (String bad : List.of("javascript:alert(1)", "file:///tmp/a", "https://", "https://a b/x", "https://user:pass@example.com/a", "http://example.com:99999/a"))
            assertThat(CatalogService.isHttpUrl(bad)).as(bad).isFalse();
        assertThatThrownBy(() -> catalog.addImage(spring.getId(), "javascript:alert(1)")).isInstanceOf(BusinessException.class);
    }
    @Test void missingIdsAreFriendly() throws Exception {
        mvc.perform(get("/courses/999999")).andExpect(status().isNotFound()).andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy khóa học")));
        mvc.perform(get("/categories/999999/edit")).andExpect(status().isNotFound());
        mvc.perform(get("/courses/abc")).andExpect(status().isBadRequest());
        mvc.perform(post("/courses/{id}/images/999999/thumbnail", spring.getId()).with(csrf())).andExpect(status().isNotFound());
        assertThatThrownBy(() -> catalog.deleteCategory(999999L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> catalog.deleteCourse(999999L)).isInstanceOf(NotFoundException.class);
    }
    @Test void rendersEveryPageIncludingNoImages() throws Exception {
        for (String path : List.of("/", "/courses", "/courses/new", "/courses/" + sql.getId(), "/courses/" + spring.getId() + "/edit", "/categories", "/categories/new", "/categories/" + web + "/edit"))
            mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(get("/courses/" + sql.getId())).andExpect(content().string(org.hamcrest.Matchers.containsString("Khóa học chưa có ảnh")));
    }
    @Test void filterErrorsAndRetainedValues() throws Exception {
        mvc.perform(get("/courses").param("name", "Spring").param("minFee", "500").param("maxFee", "100"))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Học phí tối thiểu không được lớn hơn học phí tối đa")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Spring\"")));
        mvc.perform(get("/courses").param("categoryId", "999999")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Loại khóa học không tồn tại")));
        mvc.perform(get("/courses").param("minFee", "abc")).andExpect(status().isOk());
        mvc.perform(get("/courses").param("name", "xyz-no-result")).andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy khóa học phù hợp")));
    }
    @Test void createAndEditValidationAtHttpBoundary() throws Exception {
        for (String url : List.of("/courses", "/courses/" + spring.getId())) {
            mvc.perform(post(url).with(csrf()).param("name", " ").param("categoryId", web.toString()).param("tuitionFee", "0").param("duration", "0").param("isActive", "true"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("courseForm", "name", "tuitionFee", "duration"));
            mvc.perform(post(url).with(csrf()).param("name", "Invalid").param("categoryId", "999999").param("tuitionFee", "100").param("duration", "10").param("isActive", "true"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("courseForm", "categoryId"));
        }
    }
    @Test void courseAndCategoryHttpCrudAndCsrf() throws Exception {
        mvc.perform(post("/courses").param("name", "CSRF")).andExpect(status().isForbidden());
        mvc.perform(post("/categories").with(csrf()).param("name", "HTTP Category").param("description", "New"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/categories/{id}", web).with(csrf()).param("name", "Web Updated").param("description", "Updated"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/courses").with(csrf()).param("name", "HTTP Course").param("categoryId", web.toString()).param("tuitionFee", "500").param("duration", "10").param("isActive", "true"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/courses/{id}", spring.getId()).with(csrf()).param("name", "Spring Updated").param("categoryId", web.toString()).param("tuitionFee", "1000").param("duration", "30").param("isActive", "false"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/courses/{id}/delete", react.getId()).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/categories/{id}/delete", web).with(csrf())).andExpect(flash().attribute("error", "Không thể xóa loại khóa học vì đang có khóa học thuộc loại này."));
        assertThat(catalog.course(spring.getId()).getIsActive()).isFalse();
    }
    @Test void imageHttpValidationAndDuplicateFormMessage() throws Exception {
        mvc.perform(post("/courses/{id}/images", spring.getId()).with(csrf()).param("imageUrl", "ftp://example.com/x"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("imageForm", "imageUrl"));
        mvc.perform(post("/courses").with(csrf()).param("name", "Spring Boot").param("categoryId", web.toString()).param("tuitionFee", "1000").param("duration", "10").param("isActive", "true"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("courseForm", "name"));
    }
}
