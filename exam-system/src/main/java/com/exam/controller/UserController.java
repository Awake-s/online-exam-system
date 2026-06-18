package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.dto.request.UserAddRequest;
import com.exam.dto.request.UserUpdateRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.listUsers(page, size, username, realName, roleId, classId, majorId, subjectId, grade, status));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody UserAddRequest request) {
        userService.addUser(request);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id, SecurityUtils.getCurrentUserId());
        return Result.success("删除成功", null);
    }

    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.success("操作成功", null);
    }

    @PutMapping("/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success("密码已重置为123456", null);
    }
}
