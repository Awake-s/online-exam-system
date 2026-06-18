package com.exam.dto.request;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String realName;
    private String email;
    private String phone;
    private Integer gender;
}
