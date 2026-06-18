package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.dto.request.MajorAddRequest;
import com.exam.service.MajorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/major")
public class MajorController {

    @Autowired
    private MajorService majorService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String majorName) {
        return Result.success(majorService.listMajors(page, size, majorName));
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@Valid @RequestBody MajorAddRequest request) {
        majorService.addMajor(request);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MajorAddRequest request) {
        majorService.updateMajor(id, request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        majorService.deleteMajor(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<List<Map<String, Object>>> all() {
        return Result.success(majorService.getAllMajors());
    }
}
