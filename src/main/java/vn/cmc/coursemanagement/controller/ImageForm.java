package vn.cmc.coursemanagement.controller;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ImageForm {
    @NotBlank(message = "Vui lòng nhập URL hình ảnh.")
    @Size(max = 2000, message = "URL tối đa 2000 ký tự.")
    private String imageUrl;
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
