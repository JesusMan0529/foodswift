package com.fs.service.impl;

import com.fs.context.BaseContext;
import com.fs.dto.ShoppingCartDTO;
import com.fs.entity.Dish;
import com.fs.entity.Setmeal;
import com.fs.entity.ShoppingCart;
import com.fs.mapper.DishMapper;
import com.fs.mapper.SetmealMapper;
import com.fs.mapper.ShoppingCartMapper;
import com.fs.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /***
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断当前加入到购物车中的商品是否已经存在了
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart); //通常应该只有一项

        //如果已经存在了，只需要将数量加一
        if(list != null && list.size() > 0){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else{
            //如果不存在，需要插入一条购物车数据
            //判断本次添加到购物车的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                //本次添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else{
                //本次添加到购物车的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 减少购物车中指定商品的数量。
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        if (shoppingCartDTO == null
                || (shoppingCartDTO.getDishId() == null && shoppingCartDTO.getSetmealId() == null)) {
            log.warn("忽略缺少菜品和套餐标识的购物车减菜请求：{}", shoppingCartDTO);
            return;
        }

        ShoppingCart query = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, query);
        query.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(query);
        if (list == null || list.isEmpty()) {
            // 重复点击或界面数据稍有延迟时按幂等方式处理。
            return;
        }

        ShoppingCart cart = list.get(0);
        if (cart.getNumber() != null && cart.getNumber() > 1) {
            cart.setNumber(cart.getNumber() - 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            shoppingCartMapper.deleteById(cart.getId());
        }
    }

    /***
     * 查看购物车
     * @return
     */
    public List<ShoppingCart> showShoppingCart() {
        //获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /***
     * 清空购物车
     */
    public void cleanShoppingCart() {
        //获取当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }
}
