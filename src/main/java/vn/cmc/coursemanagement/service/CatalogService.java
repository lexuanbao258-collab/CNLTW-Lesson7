package vn.cmc.coursemanagement.service;

import lombok.RequiredArgsConstructor;

import vn.cmc.coursemanagement.controller.CourseForm;
import vn.cmc.coursemanagement.entity.*;
import vn.cmc.coursemanagement.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.net.URI;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogService {
    private final CourseRepository courses;
    private final CourseCategoryRepository categories;
    private final CourseImageRepository images;


    public List<CourseCategory> categories() {
        return categories.findAll(Sort.by("id"));
    }

    public CourseCategory category(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy loại khóa học."));
    }

    public Course course(Long id) {
        return courses.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học."));
    }

    public List<Course> search(CourseFilter filter) {
        if (!filter.isFeeRangeValid())
            throw new BusinessException("feeRangeValid", "Học phí tối thiểu không được lớn hơn học phí tối đa.");
        if (filter.getCategoryId() != null && !categories.existsById(filter.getCategoryId()))
            throw new BusinessException("categoryId", "Loại khóa học không tồn tại.");
        return courses.findAll(filter.specification(), filter.ordering());
    }

    @Transactional
    public CourseCategory saveCategory(Long id, CourseCategory input) {
        CourseCategory target = id == null ? new CourseCategory() :
                categories.lockById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy loại khóa học."));
        target.setName(input.getName().strip());
        target.setDescription(input.getDescription());
        return categories.saveAndFlush(target);
    }

    @Transactional
    public void deleteCategory(Long id) {
        CourseCategory category = categories.lockById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại khóa học."));
        if (courses.existsByCategoryId(id))
            throw new BusinessException(null, "Không thể xóa loại khóa học vì đang có khóa học thuộc loại này.");
        categories.delete(category);
        categories.flush();
    }

    @Transactional
    public Course saveCourse(Long id, CourseForm input) {
        // Khóa category để thao tác kiểm tra tên và ghi dữ liệu không bị chen ngang.
        CourseCategory category = categories.lockById(input.getCategoryId())
                .orElseThrow(() -> new BusinessException("categoryId", "Loại khóa học không tồn tại."));
        String name = input.getName().strip();
        if (courses.existsByCategoryIdAndNameIgnoreCaseAndIdNot(category.getId(), name, id == null ? -1L : id))
            throw new BusinessException("name", "Tên khóa học đã tồn tại trong loại khóa học này.");
        Course course = id == null ? new Course() : courses.lockById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học."));
        course.setName(name);
        course.setCategory(category);
        course.setTuitionFee(input.getTuitionFee());
        course.setDuration(input.getDuration());
        course.setIsActive(input.getIsActive());
        course.setDescription(input.getDescription());
        return courses.saveAndFlush(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courses.lockById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học."));
        courses.delete(course);
        courses.flush();
    }

    @Transactional
    public void addImage(Long courseId, String url) {
        if (!isHttpUrl(url))
            throw new BusinessException("imageUrl", "URL ảnh phải hợp lệ và bắt đầu bằng HTTP hoặc HTTPS.");
        Course course = courses.lockById(courseId).orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học."));
        CourseImage image = new CourseImage();
        image.setCourse(course);
        image.setImageUrl(url.strip());
        image.setIsThumbnail(course.getImages().isEmpty());
        course.getImages().add(image);
        images.saveAndFlush(image);
    }

    @Transactional
    public void changeThumbnail(Long courseId, Long imageId) {
        Course course = courses.lockById(courseId).orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học."));
        CourseImage selected = images.findByIdAndCourseId(imageId, courseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hình ảnh thuộc khóa học này."));
        for (CourseImage image : course.getImages()) image.setIsThumbnail(false);
        images.flush(); // Gỡ thumbnail cũ trước khi đặt thumbnail mới; vẫn cùng transaction.
        selected.setIsThumbnail(true);
        images.flush();
    }

    public static boolean isHttpUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 2000) return false;
        try {
            URI uri = URI.create(value.strip());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() > 0 && uri.getPort() <= 65535);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
