package com.orangecrave.admin.system.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysMenuDTO {

    private Long id;
    private Long parentId;
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称过长")
    private String menuName;
    private Integer menuType;
    private String path;
    private String permKey;
    private String icon;
    private Integer sort;
}
