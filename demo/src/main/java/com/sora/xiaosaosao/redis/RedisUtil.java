package com.sora.xiaosaosao.redis;


import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Configuration
public class RedisUtil {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(redisConnectionFactory);

        FastJsonRedisSerializer serializer = new FastJsonRedisSerializer(Object.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory)  {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Resource
    RedisTemplate<String, Object> redisTemplate;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Value("${spring.redis.prefix}")
    private String KEY_PREFIX;

    /**
     * 组成最终key
     *
     * @param key
     * @return
     */
    public String generateKey(String key) {
        return KEY_PREFIX+":" + key;
    }

    /**
     * 组成最终锁KEY
     *
     * @param key
     * @return
     */
    public String generateLockKey(String key) {
        return KEY_PREFIX + "lock:" + key;
    }

    //设置key过期时间
    public void setExpire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(generateKey(key), timeout, unit);
    }

    public <T> void set(String key, T val) {
        redisTemplate.opsForValue().set(generateKey(key), val);
    }

    public <T> void set(String key, T val, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(generateKey(key), val, timeout, unit);
    }

    public <T> boolean setNX(String key, T val) {
        return redisTemplate.opsForValue().setIfAbsent(generateKey(key), val);
    }

    //累加
    public Long increment(String key, Long delta) {
        return redisTemplate.opsForValue().increment(generateKey(key), delta);
    }


    public Long increment(String key,Long time,TimeUnit timeUnit){
        Long i = redisTemplate.opsForValue().increment(key);
        if(i ==1) {
            redisTemplate.expire(key, time, timeUnit);
        }
        return i;
    }

    public <T> T get(String key) {
        Object obj = redisTemplate.opsForValue().get(generateKey(key));
        if (obj == null) {
            return null;
        } else {
            return (T) obj;
        }
    }

    public void delete(String key) {
        redisTemplate.delete(generateKey(key));
    }

    public boolean exists(String key) {
        return redisTemplate.hasKey(generateKey(key));
    }

    public <T> void putToHash(String key, Object id, T val) {
        redisTemplate.opsForHash().put(generateKey(key), id, val);
    }

    public <T> T getFromHash(String key, Object id) {
        Object obj = redisTemplate.opsForHash().get(generateKey(key), id);
        if (obj == null) {
            return null;
        } else {
            return (T) obj;
        }
    }

    public long getHashSize(String key) {
        return redisTemplate.opsForHash().size(generateKey(key));
    }

    public Map getHashMap(String key) {
        return redisTemplate.opsForHash().entries(generateKey(key));
    }

    public void deleteHashById(String key, Object id) {
        redisTemplate.opsForHash().delete(generateKey(key), id);
    }


    public <T> void leftPushList(String key, T val) {
        redisTemplate.opsForList().leftPush(generateKey(key), val);
    }

    public <T> T rightPopList(String key) {
        Object obj = redisTemplate.opsForList().rightPop(generateKey(key));
        if (obj == null) {
            return null;
        } else {
            return (T) obj;
        }
    }

    public <T> T rightPopList(String key, long time, TimeUnit timeUnit) {
        Object obj = redisTemplate.opsForList().rightPop(generateKey(key), time, timeUnit);
        if (obj == null) {
            return null;
        } else {
            return (T) obj;
        }
    }


    public void addToSet(String key, String val) {
        redisTemplate.opsForSet().add(generateKey(key), val);
    }

    public void removeFromSet(String key, String val) {
        redisTemplate.opsForSet().remove(generateKey(key), val);
    }

    public boolean existsInSet(String key, String val) {
        return redisTemplate.opsForSet().isMember(generateKey(key), val);
    }


    public void setString(String key, String val) {
        stringRedisTemplate.opsForValue().set(generateKey(key), val);
    }

    public void setString(String key, String val, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(generateKey(key), val, timeout, unit);
    }

    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(generateKey(key));
    }

    /**
     * 分布式锁
     * @param lockKey
     * @param timeout
     * @param unit
     * @param wait
     * @return
     */
    public boolean lock(String lockKey, long timeout, TimeUnit unit,Boolean wait) {
        // 是否有正在执行的线程
        String key = generateLockKey(lockKey);
        long time = System.nanoTime();
        long end_time = time + unit.toNanos(timeout);
        try {
            while (System.nanoTime() < end_time) {
                if (stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(end_time),timeout,unit)) {
//                    System.out.println("获得锁：" + key);
                    return true;
                } else {
                    /**防止崩溃导致死锁，手动校验锁有效期*/
                    String lock_expired_time = stringRedisTemplate.opsForValue().get(key);
                    if (!"".equals(lock_expired_time) && time > Long.parseLong(lock_expired_time)) {
                        /**锁已经超时,设置新超时时间，并返回拿到锁*/
                        stringRedisTemplate.expire(generateLockKey(lockKey), timeout, unit);
                        return true;
                    }

                    if(!wait){  //不等待直接返回
                        return false;
                    }
                }

                //加随机时间防止活锁
                Thread.sleep(100 + new Random().nextInt(20));
            }
        } catch (Exception e) {
            System.out.println("getLock error:" + e.toString());
            unlock(lockKey);
        }
        return false;
    }


    public void unlock(String lock_key) {
        stringRedisTemplate.delete(generateLockKey(lock_key));
//        System.out.println("释放锁：" + lock_key);
    }




    /**
     * scan 实现
     * @param pattern   表达式
     * @param consumer  对迭代到的key进行操作
     */
    public void scan(String pattern, Consumer<byte[]> consumer) {
        this.stringRedisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().count(Long.MAX_VALUE).match(pattern).build())) {
                cursor.forEachRemaining(consumer);
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 获取符合条件的key
     * @param pattern   表达式
     * @return
     */
    public List<String> keys(String pattern) {
        List<String> keys = new ArrayList<>();
        this.scan(pattern, item -> {
            //符合条件的key
            String key = new String(item, StandardCharsets.UTF_8);
            keys.add(key);
        });
        return keys;
    }

}
