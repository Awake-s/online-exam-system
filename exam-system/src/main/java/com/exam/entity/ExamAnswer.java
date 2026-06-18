package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long questionId;
    private String answer;
    private Integer isCorrect;
    private BigDecimal score;
    private String comment;
    private Integer isRemoved;
    private LocalDateTime createTime;
}
