package vn.cmc.coursemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Courses", uniqueConstraints = @UniqueConstraint(name = "uk_course_name_category", columnNames = {"CourseCategoryId", "Name"}), indexes = @Index(name = "idx_course_category", columnList = "CourseCategoryId"))
@Check(constraints = "\"TuitionFee\" > 0 AND \"Duration\" > 0")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    @Column(name = "Name", nullable = false, length = 150)
    private String name;
    @Column(name = "TuitionFee", nullable = false, precision = 14, scale = 2)
    private BigDecimal tuitionFee;
    @Column(name = "Duration", nullable = false)
    private Integer duration;
    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;
    @Column(name = "Description", length = 4000)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseCategoryId", nullable = false, foreignKey = @ForeignKey(name = "fk_course_category"))
    private CourseCategory category;
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CourseImage> images = new ArrayList<>();

    public String getFormattedTuitionFee() {
        var format = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(2);
        return format.format(tuitionFee);
    }

    public String getDurationLabel() {
        return duration <= 30 ? "Ngắn hạn" : "Dài hạn";
    }

    public CourseImage getThumbnail() {
        return images.stream().filter(image -> Boolean.TRUE.equals(image.getIsThumbnail())).findFirst().orElse(null);
    }
}
