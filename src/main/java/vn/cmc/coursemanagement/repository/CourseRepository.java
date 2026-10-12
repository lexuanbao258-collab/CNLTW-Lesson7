package vn.cmc.coursemanagement.repository;
import vn.cmc.coursemanagement.entity.Course;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    boolean existsByCategoryId(Long categoryId);
    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);
    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Course> findById(Long id);
    @EntityGraph(attributePaths = {"category", "images"})
    List<Course> findAll(org.springframework.data.jpa.domain.Specification<Course> spec, org.springframework.data.domain.Sort sort);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> lockById(Long id);
}
