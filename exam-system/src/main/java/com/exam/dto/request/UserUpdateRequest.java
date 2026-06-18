package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class UserUpdateRequest {
    @NotNull(message = "用户ID不能为空")
    private Long id;
    @Size(min = 2, max = 50)
    private String realName;
    private Long roleId;
    private Long classId;
    /** 教师关联的班级ID列表 */
    private List<Long> classIds;
    /** 教师关联的科目ID列表 */
    private List<Long> subjectIds;
    private Integer gender;
    private String email;
    private String phone;
    private Integer status;
}
