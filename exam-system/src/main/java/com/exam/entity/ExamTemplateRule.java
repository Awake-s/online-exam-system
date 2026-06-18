package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("exam_template_rule")
public class ExamTemplateRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Integer questionType;
    private Integer questionCount;
    private BigDecimal scorePerQuestion;
    private Integer difficulty;
    private Integer sortOrder;
}
