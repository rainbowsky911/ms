package com.zitai.ms.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain=true)
public class Order {
    private Integer id;

    /**
     * 库存ID
     */
    private Integer sid;
    /**
     * 商品名称
     */
    private String name;
    /**
     * 创建时间
     */
    private Date createDate;
}
