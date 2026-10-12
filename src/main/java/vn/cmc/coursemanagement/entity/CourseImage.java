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
@Table(name = "CourseImages", indexes = @Index(name = "idx_image_course", columnList = "CourseId"))

public class CourseImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    @Column(name = "ImageUrl", nullable = false, length = 2000)
    private String imageUrl;
    @Column(name = "IsThumbnail", nullable = false)
    private Boolean isThumbnail = false;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseId", nullable = false, foreignKey = @ForeignKey(name = "fk_image_course"))
    private Course course;

}
