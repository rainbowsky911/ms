package com.zitai.ms;


import com.zitai.ms.dao.OrderDao;
import com.zitai.ms.dao.StockDao;
import com.zitai.ms.dao.UserDao;
import com.zitai.ms.entity.CacheKey;
import com.zitai.ms.entity.Order;
import com.zitai.ms.entity.Stock;
import com.zitai.ms.entity.User;
import com.zitai.ms.service.OrderService;
import com.zitai.ms.service.StockService;
import com.zitai.ms.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.sun.javafx.font.FontResource.SALT;

@SpringBootTest
@RunWith(SpringRunner.class)
@WebAppConfiguration
public class MsApplicationTests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockDao stockDao;

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    /**
     * 秒杀商品库存缓存写入Reids
     */
    @Test
    public void testLoads() {
        Stock stock = stockDao.selectStock(1);
        redisTemplate.opsForValue().set("kill" + stock.getId(), stock.getId() + "");
    }

    /**
     * 乐观锁
     */
    @Test
    public void testOptimismLock() {
        Stock stock1 = stockDao.selectStock(1);
        Stock stock2 = stockDao.selectStock(1);
        stockDao.updateSale(stock1);
        int res = stockDao.updateSale(stock2);
        if (res == 0) {
            System.out.println("更新失败");
        }
    }


    /**
     * 查询缓存
     * 缓存命中,返回缓存；缓存未命中 查询数据库；更新缓存
     */
    @Test
    public void updateCacheCount() {
        int stockId = 1;
        String countKey = stringRedisTemplate.opsForValue().get(CacheKey.STOCK_COUNT + "_" + stockId);
        if (countKey != null) {
            //缓存命中
            System.out.println(countKey);
        } else {
            Stream<Integer> integerStream = Stream.of(stockDao.selectStock(stockId)).map(e -> {
                return e.getCount() - e.getSale();
            });
            Integer integer = integerStream.findFirst().get();
            Integer count = stockDao.selectStock(1).getCount();
            stringRedisTemplate.opsForValue().set(CacheKey.STOCK_COUNT + "_" + stockId, integer + "", 3600, TimeUnit.SECONDS);
        }
    }


    /**
     * 生成验证Code
     */
    @Test
    public void saveVerifyCode() {
        int stockId = 1;
        int userId = 1;
        String code = stringRedisTemplate.opsForValue().get(CacheKey.HASH_KEY + "_" + stockId);
        User user = userDao.selectByPrimaryKey(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (code == null) {
            // 生成hash
            String verify = SALT + stockId + userId + "";
            String verifyHash = DigestUtils.md5DigestAsHex(verify.getBytes());
            stringRedisTemplate.opsForValue().set(CacheKey.HASH_KEY + "_" + stockId, verifyHash, 3600, TimeUnit.SECONDS);
        } else {

            System.out.println(code);
        }
    }


    /**
     * 验证用户 -秒杀下单-减库存-创建订单-删除缓存
     */
    @Test
    public void createOrderWithVerifyCode() {
        int stockId = 1;
        int userId = 1;
        String code = stringRedisTemplate.opsForValue().get(CacheKey.HASH_KEY + "_" + stockId);

        //验证code
        if (code == null) {
            throw new RuntimeException("未验证登录");
        }

        //下单
        //校验redis商品是否在秒杀时间中
        if (!stringRedisTemplate.hasKey("kill" + stockId)) {
            throw new RuntimeException("当前商品的抢购活动已经结束！");
        }
        String count = stringRedisTemplate.opsForValue().get(CacheKey.STOCK_COUNT + "_" + stockId);
        if (count == null) {
            throw new RuntimeException("商品已下架");

        }
        int redisCount = Integer.parseInt(count);
        if (redisCount < 0) {
            throw new RuntimeException("当前商品已经秒杀完毕！");
        }

        //校验库存
        Stock stock = stockDao.selectStock(stockId);

        //创建订单
        Order order = new Order()
                .setSid(stockId)
                .setName(stock.getName())
                .setCreateDate(new Date());
        orderDao.createOrder(order);

        //更新库存

        //TODO 此处有问题，此处的库存不是最新的库存而是上条记录的库存
        stockDao.updateSale(stock);
        int afterUpdateCount = stock.getCount() - stock.getSale();
        System.out.println("商品:" + stock.getId() + "现在数据库还有" + afterUpdateCount + "件");




       /* //删除Redis库存缓存
        Boolean delete = stringRedisTemplate.delete(CacheKey.STOCK_COUNT + "_" + stockId);
        if (delete ) {
            System.out.println("删除库存缓存成功");
        }*/


        //重新设置缓存
        /***
         * 双写不一致的问题?
         * 一般主流的是先删除缓存,再去更新数据库
         *  link:https://www.yasinshaw.com/articles/74
         *
         */
        stringRedisTemplate.opsForValue().set(CacheKey.STOCK_COUNT + "_" + stockId, afterUpdateCount + "");

    }


}
