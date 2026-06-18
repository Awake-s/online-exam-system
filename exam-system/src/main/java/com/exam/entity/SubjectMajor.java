package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("subject_major")
public class SubjectMajor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subjectId;
    private Long majorId;
    private LocalDateTime createTime;
}
