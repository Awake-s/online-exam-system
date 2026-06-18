package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.dto.request.SubjectAddRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subject")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long majorId) {
        // v7 KISS 减肥: 删 courseType 查询参数 (教务化字段)
        return Result.success(subjectService.listSubjects(page, size, subjectName, grade, majorId));
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@Valid @RequestBody SubjectAddRequest request) {
        subjectService.addSubject(request);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SubjectAddRequest request) {
        subjectService.updateSubject(id, request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<List<Map<String, Object>>> all() {
        Long userId = SecurityUtils.getCurrentUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        if (isTeacher) {
            return Result.success(subjectService.getAllSubjectsByTeacher(userId));
        }
        return Result.success(subjectService.getAllSubjects());
    }
}
