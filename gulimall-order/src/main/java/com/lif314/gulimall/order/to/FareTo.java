package com.lif314.gulimall.order.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareTo {
    private MemberAddressTo address;

    private BigDecimal fare;
}
