package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

@RestController("AdminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "商店营业状态")
@Slf4j
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result setStatus(@PathVariable Integer status){

        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("SHOP_STATUS",status);
        return Result.success();
    }

@GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result<Integer> getStatus(Integer status){
    ValueOperations valueOperations = redisTemplate.opsForValue();
    Integer shop_status = (Integer) valueOperations.get("SHOP_STATUS");

    return Result.success(shop_status);
}

}
