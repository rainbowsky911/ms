package com.zitai.ms.dao;


import com.zitai.ms.entity.Stock;
import org.springframework.cache.annotation.Cacheable;

public interface  StockDao {

    @Cacheable("stock")
    //根据商品id查询库存信息的方法
    Stock checkStock(Integer id);

    //根据商品id扣除库存
    int updateSale(Stock stock);


    /**
     * 根据ID查找库存
     * @param id
     * @return
     */
    Stock  selectStock(int id);
}
