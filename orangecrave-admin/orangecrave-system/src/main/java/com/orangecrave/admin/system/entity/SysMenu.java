package com.orangecrave.admin.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;
    private String menuName;
    /** 1 目录 / 2 菜单 / 3 按钮 */
    private Integer menuType;
    private String path;
    private String permKey;
    private String icon;
    private Integer sort;
}
