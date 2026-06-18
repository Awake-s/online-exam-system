package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.dto.request.ClassAddRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class")
@PreAuthorize("hasRole('ADMIN')")
public class ClassController {

    @Autowired
    private ClassService classService;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long majorId) {
        return Result.success(classService.listClasses(page, size, className, grade, majorId));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(classService.getClassDetail(id));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody ClassAddRequest request) {
        classService.addClass(request);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ClassAddRequest request) {
        classService.updateClass(id, request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.deleteClass(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/students/{classId}")
    public Result<List<Map<String, Object>>> students(@PathVariable Long classId) {
        return Result.success(classService.getStudentsByClassId(classId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<Map<String, Object>>> all() {
        return Result.success(classService.getAllClasses());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<List<Map<String, Object>>> myClasses() {
        return Result.success(classService.getTeacherClasses(SecurityUtils.getCurrentUserId()));
    }
}
