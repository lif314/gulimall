package com.lif314.gulimall.member.vo;

import lombok.Data;



@Data
public class SocialUserVo {
    private Long socialUid;// 用户id
    private String socialType; // 社交登录类型
}
