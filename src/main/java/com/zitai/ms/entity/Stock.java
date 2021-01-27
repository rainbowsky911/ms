package com.zitai.ms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
public class Stock {

    private Integer id;

    /**
     * 名称
     */
    private String name;
    /**
     * 库存
     */
    private Integer count;
    /**
     * 已售
     */
    private Integer sale;
    /**
     * 乐观锁，版本号
     */
    private Integer version;

}
