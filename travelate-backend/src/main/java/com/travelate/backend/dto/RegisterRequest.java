package com.travelate.backend.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank(message = "手机号/邮箱不能为空")
    private String username;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    private String nickname;
    
    private String country;
    
    private String langCode;
    
    private String regionCode;
}
