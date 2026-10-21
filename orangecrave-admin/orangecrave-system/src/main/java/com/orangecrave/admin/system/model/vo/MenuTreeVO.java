package com.orangecrave.admin.system.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String path;
    private String permKey;
    private String icon;
    private Integer sort;
    private List<MenuTreeVO> children = new ArrayList<>();
}
