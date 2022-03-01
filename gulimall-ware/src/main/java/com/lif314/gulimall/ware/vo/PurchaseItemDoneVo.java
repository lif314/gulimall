package com.lif314.gulimall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {

    /**
     * {
     *    id: 123,//采购单id
     *    items: [{itemId:1,status:4,reason:""}]//完成/失败的需求详情
     * }
     */
    private Long itemId;
    private Integer status;
    private String reason;
}