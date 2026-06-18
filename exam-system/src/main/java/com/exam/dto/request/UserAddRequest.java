package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class UserAddRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50)
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20)
    private String password;
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 50)
    private String realName;
    @NotNull(message = "角色不能为空")
    private Long roleId;
    private Long classId;
    /** 教师关联的班级ID列表 */
    private List<Long> classIds;
    /** 教师关联的科目ID列表 */
    private List<Long> subjectIds;
    private Integer gender;
    private String email;
    private String phone;
    private Integer status = 1;
}
