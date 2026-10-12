package vn.cmc.coursemanagement.repository;

import vn.cmc.coursemanagement.entity.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseImageRepository extends JpaRepository<CourseImage, Long> {
    Optional<CourseImage> findByIdAndCourseId(Long id, Long courseId);
}
