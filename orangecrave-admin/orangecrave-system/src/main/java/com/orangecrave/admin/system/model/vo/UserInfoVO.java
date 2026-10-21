package com.orangecrave.admin.system.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private List<String> roles;
    private List<String> perms;
}
