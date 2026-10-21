package com.orangecrave.admin.system.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SysUserDTO {

    private Long id;
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名过长")
    private String username;
    @Size(min = 6, max = 64, message = "密码需 6~64 位")
    private String password;
    @Size(max = 50, message = "昵称过长")
    private String nickname;
    @Size(max = 20, message = "手机号过长")
    private String phone;
    private Integer status;
    private List<Long> roleIds;
}
