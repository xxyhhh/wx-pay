package com.atguigu.payment.controller;

import com.atguigu.payment.entity.Product;
import com.atguigu.payment.service.ProductService;
import com.atguigu.payment.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin //开放前端的跨域访问
@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Resource
    private ProductService productService;


    @Operation(summary = "商品列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(){

        List<Product> list = productService.list();
        HashMap<String, Object> map = new HashMap<>();
        map.put("productList", list);
        return Result.ok(map);
    }
}
