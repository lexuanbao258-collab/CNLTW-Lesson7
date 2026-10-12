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
@Table(name = "CourseCategories")

public class CourseCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    @NotBlank(message = "Tên loại khóa học không được để trống.")
    @Size(max = 150, message = "Tên tối đa 150 ký tự.")
    @Column(name = "Name", nullable = false, length = 150)
    private String name;
    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự.")
    @Column(name = "Description", length = 2000)
    private String description;
    @OneToMany(mappedBy = "category")
    private List<Course> courses = new ArrayList<>();

}
