package com.exam.entity;

import  com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("edu_class")
public class EduClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String className;
    private String grade;
    private Long majorId;
    private String description;
    private LocalDateTime createTime;
}
