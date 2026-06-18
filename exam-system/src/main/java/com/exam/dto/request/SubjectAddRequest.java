package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class SubjectAddRequest {
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;
    private String description;
    /** 旧版抽象科目多对多关联（向后兼容；具体课程模式下可不传） */
    private List<Long> majorIds;
    /** 年级，如 2022级、2024级；具体课程模式必填 */
    private String grade;
    /** 所属专业ID（具体课程模式必填） */
    private Long majorId;
    /** 总学时 (历史遗留, 前端不采集) */
    private Integer hours;
    /** 考核方式 (历史遗留, 前端不采集) */
    private String examType;
    // v7 KISS 减肥: courseType / credit / semester 三个教务化字段已物理删除
}
