package com.lif314.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrRespVo extends AttrVo {
    private String catelogName;

    private String  groupName;

    // 回显路径
    private Long[] catelogPath;
}
