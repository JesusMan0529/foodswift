package com.fs.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * 使用高德地理编码服务计算用户地址与商家之间的直线距离。
 */
@Component
public class MapDistanceComponent {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final double EARTH_RADIUS_METERS = 6_371_000D;

    @Value("${fs.delivery.amap-key:}")
    private String amapKey;

    @Value("${fs.delivery.shop-address:西安交通大学兴庆校区梧桐苑}")
    private String shopAddress;

    @Value("${fs.delivery.max-distance-meters:5000}")
    private double maxDistanceMeters;

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(3))
            .build();

    private volatile Coordinate cachedShopCoordinate;

    public boolean isWithinRange(String customerAddress) {
        // 未配置地图 Key 时不阻断推荐功能。
        if (!StringUtils.hasText(amapKey)) {
            return true;
        }

        Coordinate shop = cachedShopCoordinate;
        if (shop == null) {
            shop = geocode(shopAddress);
            cachedShopCoordinate = shop;
        }
        Coordinate customer = geocode(customerAddress);
        return calculateDistance(shop, customer) <= maxDistanceMeters;
    }

    private Coordinate geocode(String address) {
        URI uri = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                .queryParam("key", amapKey)
                .queryParam("address", address)
                .queryParam("output", "JSON")
                .build()
                .encode()
                .toUri();

        String responseBody = restTemplate.getForObject(uri, String.class);
        JSONObject response = JSON.parseObject(responseBody);
        JSONArray geocodes = response == null ? null : response.getJSONArray("geocodes");
        if (response == null || !"1".equals(response.getString("status"))
                || geocodes == null || geocodes.isEmpty()) {
            throw new IllegalStateException("无法解析地址：" + address);
        }

        String location = geocodes.getJSONObject(0).getString("location");
        if (!StringUtils.hasText(location) || !location.contains(",")) {
            throw new IllegalStateException("地图服务未返回有效坐标：" + address);
        }

        String[] parts = location.split(",");
        return new Coordinate(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
    }

    private double calculateDistance(Coordinate from, Coordinate to) {
        double latitudeDistance = Math.toRadians(to.latitude - from.latitude);
        double longitudeDistance = Math.toRadians(to.longitude - from.longitude);
        double fromLatitude = Math.toRadians(from.latitude);
        double toLatitude = Math.toRadians(to.latitude);

        double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude)
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        return EARTH_RADIUS_METERS * 2
                * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private static final class Coordinate {
        private final double longitude;
        private final double latitude;

        private Coordinate(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }
}
