package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Session> redisTemplate;

    public void save(String key, Session value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Session get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key){
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void update(String key, Session session){
        redisTemplate.opsForValue().set(key, session);
    }
}
