package com.exam.controller;

import com.exam.common.result.Result;
import com.exam.service.SpeechService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/speech")
public class SpeechController {

    @Autowired
    private SpeechService speechService;

    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(@RequestParam("audio") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return Result.error(400, "请上传音频文件");
        }
        if (audioFile.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "音频文件不能超过 10MB");
        }

        try {
            String text = speechService.recognize(audioFile);
            Map<String, Object> result = new HashMap<>();
            result.put("text", text);
            return Result.success("识别成功", result);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
