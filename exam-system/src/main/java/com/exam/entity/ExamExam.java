package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_exam")
public class ExamExam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String examName;
    private Long paperId;
    private Long classId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long creatorId;
    private Integer status;
    private Integer scorePublished;
    /**
     * 最后一次成绩发布时间（同时作为乐观锁版本号）。
     * NULL 表示从未发布；非 NULL 表示已发布，值即最后一次发布/重发时间。
     * 用于：1) 通知文案区分首发/重发；2) CAS 防并发双击。
     */
    private LocalDateTime lastPublishTime;
    private String antiCheatConfig;
    private LocalDateTime createTime;
}
