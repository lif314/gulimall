package com.lif314.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergeVo {

    private Long purchaseId;

    private List<Long> items;

}
