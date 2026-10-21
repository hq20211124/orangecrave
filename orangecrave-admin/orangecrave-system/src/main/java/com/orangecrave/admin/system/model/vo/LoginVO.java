package com.orangecrave.admin.system.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private String tokenName;
}
