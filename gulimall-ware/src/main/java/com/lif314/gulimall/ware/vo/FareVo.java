package com.lif314.gulimall.ware.vo;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 获取收货人信息
 *
 * 运费
 * 收货人地址信息
 */
@Data
public class FareVo {

    private MemberAddressVo address;

    private BigDecimal fare;
}
