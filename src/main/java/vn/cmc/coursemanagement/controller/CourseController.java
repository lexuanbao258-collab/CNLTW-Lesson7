package vn.cmc.coursemanagement.controller;

import jakarta.validation.Valid;
import vn.cmc.coursemanagement.repository.CourseFilter;
import vn.cmc.coursemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
@RequestMapping("/courses")
public class CourseController {
    private final CatalogService catalog;

    public CourseController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public String list(@Valid @ModelAttribute("filter") CourseFilter filter, BindingResult errors, Model model) {
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("courses", java.util.List.of());
        if (!errors.hasErrors()) {
            try {
                model.addAttribute("courses", catalog.search(filter));
            } catch (BusinessException ex) {
                errors.rejectValue(ex.getField(), "business", ex.getMessage());
            }
        }
        return "courses/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("courseForm", new CourseForm());
        return form(null, model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        var course = catalog.course(id);
        CourseForm input = new CourseForm();
        input.setName(course.getName());
        input.setCategoryId(course.getCategory().getId());
        input.setTuitionFee(course.getTuitionFee());
        input.setDuration(course.getDuration());
        input.setIsActive(course.getIsActive());
        input.setDescription(course.getDescription());
        model.addAttribute("courseForm", input);
        return form(id, model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("courseForm") CourseForm input, BindingResult errors,
                         Model model, RedirectAttributes flash) {
        return save(null, input, errors, model, flash);
    }

    @PostMapping("/{id}")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("courseForm") CourseForm input,
                       BindingResult errors, Model model, RedirectAttributes flash) {
        catalog.course(id);
        return save(id, input, errors, model, flash);
    }

    private String save(Long id, CourseForm input, BindingResult errors, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                var saved = catalog.saveCourse(id, input);
                flash.addFlashAttribute("success", id == null ? "Đã thêm khóa học." : "Đã cập nhật khóa học.");
                return "redirect:/courses/" + saved.getId();
            } catch (BusinessException ex) {
                errors.rejectValue(ex.getField(), "business", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("conflict", "Dữ liệu xung đột. Hãy kiểm tra tên khóa học và loại khóa học.");
            }
        }
        return form(id, model);
    }

    private String form(Long id, Model model) {
        model.addAttribute("courseId", id);
        model.addAttribute("categories", catalog.categories());
        return "courses/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("imageForm", new ImageForm());
        return detailPage(id, model);
    }

    private String detailPage(Long id, Model model) {
        model.addAttribute("course", catalog.course(id));
        return "courses/detail";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        catalog.deleteCourse(id);
        flash.addFlashAttribute("success", "Đã xóa khóa học và các ảnh liên quan.");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/images")
    public String addImage(@PathVariable Long id, @Valid @ModelAttribute("imageForm") ImageForm input,
                           BindingResult errors, Model model, RedirectAttributes flash) {
        catalog.course(id);
        if (!errors.hasErrors()) {
            try {
                catalog.addImage(id, input.getImageUrl());
                flash.addFlashAttribute("success", "Đã thêm hình ảnh.");
                return "redirect:/courses/" + id;
            } catch (BusinessException ex) {
                errors.rejectValue(ex.getField(), "business", ex.getMessage());
            }
        }
        return detailPage(id, model);
    }

    @PostMapping("/{id}/images/{imageId}/thumbnail")
    public String thumbnail(@PathVariable Long id, @PathVariable Long imageId, RedirectAttributes flash) {
        catalog.changeThumbnail(id, imageId);
        flash.addFlashAttribute("success", "Đã đổi ảnh đại diện.");
        return "redirect:/courses/" + id;
    }
}
