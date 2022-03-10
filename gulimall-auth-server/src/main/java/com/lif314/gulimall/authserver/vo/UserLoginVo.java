package com.lif314.gulimall.authserver.vo;

import lombok.Data;

@Data
public class UserLoginVo {
    private String loginacct; // 登录账号
    private String password;
}
