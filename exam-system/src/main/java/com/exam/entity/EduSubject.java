package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("edu_subject")
public class EduSubject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String subjectName;
    /** 年级，如 2022级、2024级 */
    private String grade;
    /** 所属专业ID（FK edu_major.id）*/
    private Long majorId;
    /** 总学时 (历史遗留字段, 前端不展示也不采集; 暂保留 schema 列, 不影响 OES 业务) */
    private Integer hours;
    /** 考核方式 (历史遗留字段, 前端不展示也不采集; 暂保留 schema 列) */
    private String examType;
    /** 课程描述（可选自由文本：学分/学期/类型若需展示, 由管理员自行写入） */
    private String description;
    private LocalDateTime createTime;
    // v7 KISS 减肥: 物理删除 courseType / credit / semester 三个教务化字段
    // 理由: 学分/学期/课程类型是教务系统的 System of Record, OES 重复维护只会数据漂移
    // 详见: sql/真实场景种子数据/15_精简edu_subject去教务化字段.sql
}
