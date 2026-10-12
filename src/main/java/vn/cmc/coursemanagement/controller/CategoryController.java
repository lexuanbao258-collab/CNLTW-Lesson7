package vn.cmc.coursemanagement.controller;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import vn.cmc.coursemanagement.entity.CourseCategory;
import vn.cmc.coursemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CatalogService catalog;


    @InitBinder("categoryForm")
    public void allowFields(WebDataBinder binder) {
        binder.setAllowedFields("name", "description");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", catalog.categories());
        return "categories/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("categoryForm", new CourseCategory());
        model.addAttribute("categoryId", null);
        return "categories/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("categoryForm", catalog.category(id));
        model.addAttribute("categoryId", id);
        return "categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CourseCategory input, BindingResult errors,
                         Model model, RedirectAttributes flash) {
        return save(null, input, errors, model, flash);
    }

    @PostMapping("/{id}")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("categoryForm") CourseCategory input,
                       BindingResult errors, Model model, RedirectAttributes flash) {
        catalog.category(id);
        return save(id, input, errors, model, flash);
    }

    private String save(Long id, CourseCategory input, BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("categoryId", id);
            return "categories/form";
        }
        catalog.saveCategory(id, input);
        flash.addFlashAttribute("success", "Đã lưu loại khóa học.");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            catalog.deleteCategory(id);
            flash.addFlashAttribute("success", "Đã xóa loại khóa học.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Không thể xóa loại khóa học vì đang có khóa học thuộc loại này.");
        }
        return "redirect:/categories";
    }
}
