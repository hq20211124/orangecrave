package com.orangecrave.admin.system.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SysRoleDTO {

    private Long id;
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称过长")
    private String roleName;
    @Size(max = 100, message = "角色标识过长")
    private String roleKey;
    @Size(max = 500, message = "备注过长")
    private String remark;
    private Integer status;
    private List<Long> menuIds;
}
