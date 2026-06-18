package com.exam.dto.request;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamPublishRequest {
    @NotBlank(message = "考试名称不能为空")
    @Size(min = 2, max = 200)
    private String examName;
    @NotNull(message = "试卷不能为空")
    private Long paperId;
    private Long classId;
    private List<Long> classIds;
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private AntiCheatConfig antiCheat;

    @Data
    public static class AntiCheatConfig {
        private Integer switchScreenMax;
        private Boolean shuffleQuestion;
        private Boolean shuffleOption;
        private Boolean fullscreenRequired;
        private Boolean noCopyPaste;
        private Integer inactivityTimeout;
    }
}
