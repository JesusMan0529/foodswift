package com.fs.service;

import com.fs.dto.ShoppingCartDTO;
import com.fs.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /***
     * 添加购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 减少当前用户购物车中指定商品的数量，数量减到 0 时删除记录。
     *
     * @param shoppingCartDTO 菜品或套餐标识及口味
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /***
     * 查看购物车
     * @return
     */
    List<ShoppingCart> showShoppingCart();

    /***
     * 清空购物车
     */
    void cleanShoppingCart();
}
