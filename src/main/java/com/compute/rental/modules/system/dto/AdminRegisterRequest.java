package com.compute.rental.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRegisterRequest(
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度在3-50之间")
    String userName,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度至少为6位")
    String password,

    @NotBlank(message = "角色不能为空")
    String role
) {}
