package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MarkingScoreRequest {
    @NotNull private Long recordId;
    @NotEmpty private List<ScoreItem> scores;

    @Data
    public static class ScoreItem {
        @NotNull private Long answerId;
        @NotNull @DecimalMin("0") private BigDecimal score;
        @Size(max = 500) private String comment;
    }
}
