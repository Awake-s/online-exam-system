package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperCreateRequest {
    @NotBlank(message = "试卷名称不能为空")
    @Size(min = 2, max = 200)
    private String paperName;
    @NotNull(message = "科目不能为空")
    private Long subjectId;
    @NotNull(message = "及格分不能为空")
    private BigDecimal passScore;
    @NotNull(message = "考试时长不能为空")
    @Min(1)
    private Integer duration;
    @NotEmpty(message = "题目列表不能为空")
    private List<PaperQuestionItem> questions;

    @Data
    public static class PaperQuestionItem {
        @NotNull private Long questionId;
        @NotNull @DecimalMin("0.01") private BigDecimal score;
        @NotNull @Min(0) private Integer sortOrder;
    }
}
