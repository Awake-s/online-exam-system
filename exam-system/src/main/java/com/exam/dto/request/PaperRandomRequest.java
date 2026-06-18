package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperRandomRequest {
    @NotBlank private String paperName;
    @NotNull private Long subjectId;
    @NotNull private BigDecimal passScore;
    @NotNull @Min(1) private Integer duration;
    @NotEmpty private List<RandomRule> rules;

    @Data
    public static class RandomRule {
        @NotNull private Integer questionType;
        @NotNull @Min(1) private Integer count;
        @NotNull @DecimalMin("0.01") private BigDecimal scorePerQuestion;
        private Integer difficulty;
    }
}
