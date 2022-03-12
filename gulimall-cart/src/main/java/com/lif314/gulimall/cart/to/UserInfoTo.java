package com.lif314.gulimall.cart.to;

import lombok.Data;

/**
 * 未登陆前的
 * 临时用户
 */
@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;

    // 是否有临时用户
    private boolean tempUser = false;

}
