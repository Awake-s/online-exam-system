package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.dto.request.PaperCreateRequest;
import com.exam.dto.request.PaperRandomRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/paper")
@PreAuthorize("hasRole('TEACHER')")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String paperName,
            @RequestParam(required = false) Integer status) {
        return Result.success(paperService.listPapers(page, size, subjectId, paperName, status, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(paperService.getPaperDetail(id, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody PaperCreateRequest request) {
        paperService.createPaper(request, SecurityUtils.getCurrentUserId());
        return Result.success("创建成功", null);
    }

    @PostMapping("/random")
    public Result<Void> random(@Valid @RequestBody PaperRandomRequest request) {
        paperService.randomPaper(request, SecurityUtils.getCurrentUserId());
        return Result.success("随机组卷成功", null);
    }

    @PutMapping("/update/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PaperCreateRequest request) {
        paperService.updatePaper(id, request, SecurityUtils.getCurrentUserId());
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paperService.deletePaper(id, SecurityUtils.getCurrentUserId());
        return Result.success("删除成功", null);
    }

    @PutMapping("/togglePublish/{id}")
    public Result<Void> togglePublish(@PathVariable Long id) {
        paperService.togglePublish(id, SecurityUtils.getCurrentUserId());
        return Result.success("操作成功", null);
    }
}
