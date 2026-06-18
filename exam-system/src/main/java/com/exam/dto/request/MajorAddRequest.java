package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;

@Data
public class MajorAddRequest {
    @NotBlank(message = "专业名称不能为空")
    @Size(min = 2, max = 100)
    private String majorName;
    private String description;
}
