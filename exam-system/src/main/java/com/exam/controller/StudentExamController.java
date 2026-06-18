package com.exam.controller;

import com.exam.common.result.Result;
import com.exam.dto.request.ExamSubmitRequest;
import com.exam.entity.SysUser;
import com.exam.mapper.SysUserMapper;
import com.exam.security.SecurityUtils;
import com.exam.service.StudentExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/exam")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {

    @Autowired
    private StudentExamService studentExamService;
    @Autowired
    private SysUserMapper userMapper;

    @GetMapping("/my-exams")
    public Result<List<Map<String, Object>>> myExams() {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        return Result.success(studentExamService.getMyExams(userId, user != null ? user.getClassId() : null));
    }

    @GetMapping("/start/{examId}")
    public Result<Map<String, Object>> start(@PathVariable Long examId) {
        return Result.success(studentExamService.startExam(examId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@Valid @RequestBody ExamSubmitRequest request) {
        return Result.success(studentExamService.submitExam(request, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/result/{recordId}")
    public Result<Map<String, Object>> result(@PathVariable Long recordId) {
        return Result.success(studentExamService.getExamResult(recordId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/auto-save")
    public Result<Void> autoSave(@RequestBody ExamSubmitRequest request) {
        studentExamService.autoSaveAnswers(request, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @PostMapping("/switch-screen/{recordId}")
    public Result<Map<String, Object>> switchScreen(@PathVariable Long recordId) {
        return Result.success(studentExamService.recordSwitchScreen(recordId, SecurityUtils.getCurrentUserId()));
    }
}
