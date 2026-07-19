package com.fs.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fs.constant.MessageConstant;
import com.fs.exception.OrderBusinessException;
import com.fs.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 地图距离组件：校验收货地址是否超出配送范围
 */
@Slf4j
@Component
public class MapDistanceComponent {

    /**
     * 店铺经纬度在 Redis 中的缓存 key， 减少百度 api 调用次数
     */
    public static final String SHOP_LOCATION_KEY = "SHOP_LOCATION";

    @Value("${fs.shop.address}")
    private String shopAddress;

    @Value("${fs.baidu.ak}")
    private String ak;

    /**
     * 配送范围阈值（单位：米），在 fs-server/src/main/resources/application-dev.yml 中可以调整
     */
    @Value("${fs.shop.delivery-range:5000}")
    private Integer deliveryRange;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    public void checkOutOfRange(String address) {
        //获取店铺的经纬度坐标（优先读取 Redis 缓存，避免每次调用都解析店铺地址）
        String shopLngLat = (String) redisTemplate.opsForValue().get(SHOP_LOCATION_KEY);

        if (shopLngLat == null) {
            Map shopMap = new HashMap();
            shopMap.put("address", shopAddress);
            shopMap.put("output", "json");
            shopMap.put("ak", ak);

            //文字地址 → 经纬度
            String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", shopMap);

            JSONObject shopJsonObject = JSON.parseObject(shopCoordinate);
            if (!shopJsonObject.getString("status").equals("0")) {
                throw new OrderBusinessException("店铺地址解析失败");
            }

            //数据解析
            JSONObject shopLocation = shopJsonObject.getJSONObject("result").getJSONObject("location");
            String shopLat = shopLocation.getString("lat"); //纬度
            String shopLng = shopLocation.getString("lng"); //经度

            //店铺经纬度坐标
            shopLngLat = shopLat + "," + shopLng;

            //缓存店铺经纬度，有效期24小时
            redisTemplate.opsForValue().set(SHOP_LOCATION_KEY, shopLngLat, 24, TimeUnit.HOURS);
        }

        Map map = new HashMap();
        map.put("address", address);
        map.put("output", "json");
        map.put("ak", ak);

        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > deliveryRange) {
            //配送距离超过阈值
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
