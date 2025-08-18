package com.booking.booking.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

  private final RedisTemplate<String, Object> redisTemplate;

  public void setWithTTL(String key, Object value, long ttlInSeconds) {
    redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlInSeconds));
  }

  public Object get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  public long getTTL(String key) {
    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    return ttl != null ? ttl : -999;
  }
}
