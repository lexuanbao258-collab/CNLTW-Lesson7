package vn.cmc.coursemanagement.repository;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CourseFilter {
    @Size(max = 150, message = "Từ khóa tối đa 150 ký tự.")
    private String name;

    private Long categoryId;

    private Boolean isActive;
    @DecimalMin(value = "0", message = "Học phí tối thiểu không được âm.")
    private BigDecimal minFee;
    @DecimalMin(value = "0", message = "Học phí tối đa không được âm.")
    private BigDecimal maxFee;
    @Pattern(regexp = "id|feeAsc|feeDesc|durationAsc", message = "Kiểu sắp xếp không hợp lệ.")
    private String sort = "id";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public BigDecimal getMinFee() {
        return minFee;
    }

    public void setMinFee(BigDecimal minFee) {
        this.minFee = minFee;
    }

    public BigDecimal getMaxFee() {
        return maxFee;
    }

    public void setMaxFee(BigDecimal maxFee) {
        this.maxFee = maxFee;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    @AssertTrue(message = "Học phí tối thiểu không được lớn hơn học phí tối đa.")
    public boolean isFeeRangeValid() {
        return minFee == null || maxFee == null || minFee.compareTo(maxFee) <= 0;
    }

    public org.springframework.data.jpa.domain.Specification<vn.cmc.coursemanagement.entity.Course> specification() {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (name != null && !name.isBlank()) {
                String literal = name.strip().toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + literal + "%", '\\'));
            }
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (isActive != null) predicates.add(cb.equal(root.get("isActive"), isActive));
            if (minFee != null) predicates.add(cb.greaterThanOrEqualTo(root.get("tuitionFee"), minFee));
            if (maxFee != null) predicates.add(cb.lessThanOrEqualTo(root.get("tuitionFee"), maxFee));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public org.springframework.data.domain.Sort ordering() {
        var order = switch (sort) {
            case "feeAsc" -> org.springframework.data.domain.Sort.by("tuitionFee").ascending();
            case "feeDesc" -> org.springframework.data.domain.Sort.by("tuitionFee").descending();
            case "durationAsc" -> org.springframework.data.domain.Sort.by("duration").ascending();
            default -> org.springframework.data.domain.Sort.by("id");
        };
        return order.and(org.springframework.data.domain.Sort.by("id"));
    }
}
