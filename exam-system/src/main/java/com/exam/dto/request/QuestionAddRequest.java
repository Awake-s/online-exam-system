package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionAddRequest {
    @NotNull(message = "科目不能为空")
    private Long subjectId;
    @NotNull(message = "题型不能为空")
    @Min(1) @Max(5)
    private Integer questionType;
    @NotBlank(message = "题目内容不能为空")
    private String content;
    private List<String> options;
    @NotBlank(message = "答案不能为空")
    private String answer;
    private String analysis;
    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.01")
    private BigDecimal score;
    @NotNull(message = "难度不能为空")
    @Min(1) @Max(3)
    private Integer difficulty;
}
