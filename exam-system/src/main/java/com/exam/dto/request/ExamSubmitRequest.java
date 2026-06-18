package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class ExamSubmitRequest {
    @NotNull private Long recordId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        @NotNull private Long questionId;
        private String answer;
    }
}
