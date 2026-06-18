package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;

@Data
public class PasswordChangeRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20)
    private String newPassword;
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
