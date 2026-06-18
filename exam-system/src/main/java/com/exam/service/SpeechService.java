package com.exam.service;

import org.springframework.web.multipart.MultipartFile;

public interface SpeechService {
    /**
     * 语音转文字
     * @param audioFile 音频文件（支持 wav, pcm, webm 格式）
     * @return 识别出的文字
     */
    String recognize(MultipartFile audioFile);
}
