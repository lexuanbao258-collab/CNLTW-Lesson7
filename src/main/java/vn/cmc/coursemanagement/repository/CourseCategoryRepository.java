package vn.cmc.coursemanagement.repository;

import vn.cmc.coursemanagement.entity.CourseCategory;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    Optional<CourseCategory> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseCategory c where c.id = :id")
    Optional<CourseCategory> lockById(Long id);
}
