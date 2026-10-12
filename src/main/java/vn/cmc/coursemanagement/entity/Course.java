package vn.cmc.coursemanagement.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Courses", uniqueConstraints = @UniqueConstraint(name = "uk_course_name_category", columnNames = {"CourseCategoryId", "Name"}), indexes = @Index(name = "idx_course_category", columnList = "CourseCategoryId"))
@Check(constraints = "\"TuitionFee\" > 0 AND \"Duration\" > 0")
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CourseCategory getCategory() { return category; }
    public void setCategory(CourseCategory category) { this.category = category; }
    public List<CourseImage> getImages() { return images; }
    public void setImages(List<CourseImage> images) { this.images = images; }
    public String getFormattedTuitionFee() {
        var format = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(2);
        return format.format(tuitionFee);
    }
    public String getDurationLabel() { return duration <= 30 ? "Ngắn hạn" : "Dài hạn"; }
    public CourseImage getThumbnail() {
        return images.stream().filter(image -> Boolean.TRUE.equals(image.getIsThumbnail())).findFirst().orElse(null);
    }
}
