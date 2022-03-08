package com.lif314.gulimall.product.vo;

import com.lif314.gulimall.product.saveVo.Attr;
import lombok.Data;

import java.util.List;

@Data
public  class SpuItemAttrGroupVo {
    // 分组信息
    private String groupName;
    private List<Attr> attrs;
}
