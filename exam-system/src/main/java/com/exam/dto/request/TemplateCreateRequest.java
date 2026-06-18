package com.exam.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TemplateCreateRequest {
    @NotBlank(message = "模板名称不能为空")
    @Size(min = 2, max = 100)
    private String templateName;

    @NotNull(message = "科目不能为空")
    private Long subjectId;

    private BigDecimal targetScore;
    private BigDecimal passScore;
    private Integer duration;
    private String description;

    @NotEmpty(message = "题型规则不能为空")
    private List<RuleItem> rules;

    @Data
    public static class RuleItem {
        @NotNull(message = "题型不能为空")
        private Integer questionType;

        @NotNull(message = "题目数量不能为空")
        @Min(1)
        private Integer questionCount;

        @NotNull(message = "每题分值不能为空")
        @DecimalMin("0.01")
        private BigDecimal scorePerQuestion;

        private Integer difficulty;
        private Integer sortOrder;
    }
}
