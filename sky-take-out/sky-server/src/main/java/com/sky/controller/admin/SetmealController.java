package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {
@Autowired
private SetmealService setmealService;

    @PostMapping()
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = "setmealCache" ,key = "#setmealDTO.categoryId")
    public Result saveWithDish(@RequestBody SetmealDTO setmealDTO){

setmealService.saveWithDish(setmealDTO);

        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
       PageResult pageResult= setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("删除套餐")
    @CacheEvict(cacheNames = "setmealCache" ,allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        setmealService.delete(ids);
        return Result.success();
    }

@GetMapping("/{id}")
@ApiOperation("根据id查套餐")
public Result<SetmealVO> getById(@PathVariable Long id){
        SetmealVO setmeal=setmealService.getById(id);
        return Result.success(setmeal);
}

    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmealCache" ,allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){

        setmealService.update(setmealDTO);
        return Result.success();
    }
@PostMapping("/status/{status}")
    @ApiOperation("套餐起售停售")
@CacheEvict(cacheNames = "setmealCache" ,allEntries = true)
    public Result startOeStop(@PathVariable Integer status,Long id){
        setmealService.startOrStop(status,id);
        return Result.success();
}
}
