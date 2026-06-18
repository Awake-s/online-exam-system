package com.exam.dto.request;
import lombok.Data;
import javax.validation.constraints.*;

@Data
public class ClassAddRequest {
    @NotBlank(message = "班级名称不能为空")
    @Size(min = 2, max = 100)
    private String className;
    private String grade;
    private Long majorId;
    private String description;
}
