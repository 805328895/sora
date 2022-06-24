package com.sora.watcher.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import com.sora.watcher.config.DataConfig;
import com.sora.watcher.config.RedisConfig;
import com.sora.watcher.config.SoraModel;
import javafx.scene.control.Tab;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Component
@Configuration
public class RedisUtils {

    private static RedisTemplate redisTemplate = null;

    private static RedisConfig _redisConfig;


    @Bean
    @SuppressWarnings("rawtypes")
    public RedisSerializer fastJson2JsonRedisSerializer() {
        return new FastJson2JsonRedisSerializer<Object>(Object.class);
    }

    public static void init(RedisConfig redisConfig){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        JedisConnectionFactory fac = new JedisConnectionFactory(new JedisPoolConfig());
        JedisShardInfo shardInfo = new JedisShardInfo(redisConfig.getHost(), redisConfig.getPort());
        shardInfo.setPassword(redisConfig.getPassword());
        fac.setShardInfo(shardInfo);

        RedisSerializer serializer = new FastJson2JsonRedisSerializer<Object>(Object.class);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setConnectionFactory(fac);
        template.afterPropertiesSet();
        redisTemplate = template;
        _redisConfig =redisConfig;
    }

    public static void set(SoraModel model) {
        redisTemplate.opsForValue().set(_redisConfig.getPrefix()+":"+ model.getDbName(), model);
    }

    public static  <T> T get(String key) {
        T t =null;
        Object obj = redisTemplate.opsForValue().get(_redisConfig.getPrefix()+":"+key);
        if (obj == null) {
            return null;
        }

        return (T) obj;

    }


    private static Class<?>  getGenericClass(Class<?> targetClass) {
        if (targetClass == Object.class)
            return null;

        Type[] types = targetClass.getGenericInterfaces();
        if (types.length == 0) {
            types = new Type[] {targetClass.getGenericSuperclass()};
        }

        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) type;
                Type[] array = t.getActualTypeArguments();
                return (Class<?>) array[0];
            }
        }

        return getGenericClass(targetClass.getSuperclass());
    }
}
