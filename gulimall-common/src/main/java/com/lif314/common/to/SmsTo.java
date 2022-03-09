package com.lif314.common.to;

import lombok.Data;

/**
 * 验证码校验
 */
@Data
public class SmsTo {
    private String phone;
    private String code;
}
