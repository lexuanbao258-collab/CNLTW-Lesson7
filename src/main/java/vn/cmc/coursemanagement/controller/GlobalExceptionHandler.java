package vn.cmc.coursemanagement.controller;

import vn.cmc.coursemanagement.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException ex) {
        return error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView invalidId(MethodArgumentTypeMismatchException ex) {
        return error("Địa chỉ không hợp lệ. ID phải là số.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException ex) {
        return error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView conflict(DataIntegrityViolationException ex) {
        return error("Dữ liệu đang được sử dụng hoặc bị trùng. Vui lòng tải lại trang và kiểm tra.", HttpStatus.CONFLICT);
    }

    private ModelAndView error(String message, HttpStatus status) {
        ModelAndView view = new ModelAndView("error");
        view.addObject("message", message);
        view.setStatus(status);
        return view;
    }
}
