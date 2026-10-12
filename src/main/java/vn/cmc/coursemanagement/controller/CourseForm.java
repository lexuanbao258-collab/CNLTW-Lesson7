package vn.cmc.coursemanagement.controller;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CourseForm {
    @NotBlank(message = "Tên khóa học không được để trống.")
    @Size(max = 150, message = "Tên tối đa 150 ký tự.")
    private String name;
    @NotNull(message = "Vui lòng chọn loại khóa học.")
    private Long categoryId;
    @NotNull(message = "Vui lòng nhập học phí.")
    @DecimalMin(value = "0", inclusive = false, message = "Học phí phải lớn hơn 0.")
    @Digits(integer = 12, fraction = 2, message = "Học phí tối đa 12 chữ số và 2 chữ số thập phân.")
    private BigDecimal tuitionFee;
    @NotNull(message = "Vui lòng nhập thời lượng.")
    @Positive(message = "Thời lượng phải lớn hơn 0.")
    private Integer duration;
    @NotNull(message = "Vui lòng chọn trạng thái.")
    private Boolean isActive = true;
    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự.")
    private String description;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
