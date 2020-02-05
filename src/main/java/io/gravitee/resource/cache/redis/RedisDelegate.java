/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gravitee.resource.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.policy.cache.CacheResponse;
import io.gravitee.policy.cache.resource.CacheElement;
import io.gravitee.resource.cache.Cache;
import io.gravitee.resource.cache.Element;
import io.gravitee.resource.cache.configuration.CacheResourceConfiguration;
import io.gravitee.resource.cache.configuration.RedisCacheResponse;
import io.gravitee.resource.cache.configuration.RedisHost;
import org.springframework.core.env.Environment;
import redis.clients.jedis.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;

public class RedisDelegate implements Cache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDelegate.class);
    private static CacheResourceConfiguration configuration;
    private JedisPool jedisPool;
    JedisCluster jedisCluster = null;
    private final static char KEY_SEPARATOR = '_';
    private final Boolean isRedisCluster;
    private static Environment environment;
    private Integer redisDatabase;

    public RedisDelegate(CacheResourceConfiguration conf, Environment environment) {
        //final JedisPoolConfig poolConfig = buildPoolConfig();
        this.configuration = conf;
        this.environment = environment;
        this.redisDatabase = configuration.getRedisDatabase();
        //jedisPool = JedisPoolSingleton.getInstance();
        isRedisCluster = configuration.isRedisCluster();
        if (isRedisCluster) {
            jedisCluster = JedisPoolSingleton.getJedisCluster();
        } else {
            jedisPool = JedisPoolSingleton.getInstance();
        }
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public Object getNativeCache() {
        return null;
    }

    @Override
    public Element get(Object key) {
        Element element = null;
        try {
            String newKey = buildKey(key.toString());
            String value = null;
            if (isRedisCluster) {
                if (jedisCluster != null) {
                    value = jedisCluster.get(newKey);
                }
                else{
                    LOGGER.error("Jedis cluster couldn't be initialized");
                }
            } else {
                if (jedisPool != null) {
                    Jedis jedis = jedisPool.getResource();
                    jedis.select(redisDatabase);
                    value = jedis.get(newKey);
                }
                else{
                    LOGGER.error("Jedis pool couldn't be initialized");
                }
            }
            if (value != null) {
                CacheResponse response = new CacheResponse();
                RedisCacheResponse redisCacheResponse = new RedisCacheResponse();
                ObjectMapper mapperObj = new ObjectMapper();
                try {
                    redisCacheResponse = mapperObj.readValue(value, RedisCacheResponse.class);
                    if (redisCacheResponse != null) {
                        HttpHeaders headers = mapperObj.readValue(redisCacheResponse.getHeaders(), HttpHeaders.class);
                        byte[] content = Base64.getDecoder().decode(redisCacheResponse.getContent());
                        String originalKey = redisCacheResponse.getOriginalKey();
                        response.setStatus(redisCacheResponse.getStatus());
                        response.setHeaders(headers);
                        response.setContent(Buffer.buffer(content));
                        element = new CacheElement(originalKey, response);
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.error(e.getMessage());
                }
            }
            else{
                LOGGER.info("No value found for a key: {}", newKey);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return element;
    }

    @Override
    public void put(Element element) {
        //We have an Element.. We need to serialize it..
        try {
            String jsonObject = null;
            String key = element.key().toString();
            RedisCacheResponse redisCacheResponse = new RedisCacheResponse();
            String newKey = buildKey(key);
            CacheResponse response = (CacheResponse) element.value();
            Buffer buffer = response.getContent();

            //Convert Byte[] content to base64 encoded string
            String base64Buffer = Base64.getEncoder().encodeToString(buffer.getBytes());

            //Convert HttpHeaders to json
            ObjectMapper mapperObj = new ObjectMapper();
            String headersJson = mapperObj.writeValueAsString(response.getHeaders());

            redisCacheResponse.setContent(base64Buffer);
            redisCacheResponse.setStatus(response.getStatus());
            redisCacheResponse.setHeaders(headersJson);
            redisCacheResponse.setOriginalKey(key);
            jsonObject = mapperObj.writeValueAsString(redisCacheResponse);

            /*
            LOGGER.debug("FINAL: key is:{}", newKey);
            LOGGER.debug("FINAL: value is:{}", jsonObject);
            LOGGER.debug("FINAL: TTL is:{}", element.timeToLive());
            */
            if (isRedisCluster) {
                if (jedisCluster != null) {
                    jedisCluster.set(newKey, jsonObject);
                    jedisCluster.expire(newKey, element.timeToLive());
                }
                else{
                    LOGGER.error("Jedis cluster couldn't be initialized");
                }
            } else {
                if (jedisPool != null) {
                    Jedis jedis = jedisPool.getResource();
                    jedis.select(redisDatabase);
                    jedis.set(newKey, jsonObject);
                    jedis.expire(newKey, element.timeToLive());
                }
                else{
                    LOGGER.error("Jedis pool couldn't be initialized");
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

    }

    @Override
    public void evict(Object key) {

    }

    @Override
    public void clear() {

    }

    public String buildKey(String keyFromCachePolicy) {
        String newKey = null;
        StringBuilder sb = new StringBuilder();
        newKey = sb.append(configuration.getName()).append(KEY_SEPARATOR).append(keyFromCachePolicy).toString();
        return newKey;
    }

    public static List<RedisHost> getRedisNodesFromConfig() {
        return configuration.getRedisNodes(environment);
    }

    public static class JedisPoolSingleton {
        final static JedisPoolConfig poolConfig = buildPoolConfig();
        private static JedisPool instance;
        private static JedisCluster clusterInstance;

        //make the constructor private so that this class cannot be instantiated
        private JedisPoolSingleton() {
        }

        //Get the only object available
        public static JedisPool getInstance() {
            try{
                List<RedisHost> redisHostsConfig = getRedisNodesFromConfig();
                if (redisHostsConfig.size() == 1) {
                    RedisHost hostconf = redisHostsConfig.get(0);
                    //We have a single instance of Redis
                    instance = new JedisPool(poolConfig, hostconf.getHost(), hostconf.getPort());
                } else if (redisHostsConfig.size() > 1) {
                    //We have a cluster
                    LOGGER.error("RedisPool is only available for the Single instance of redis server");
                }
                return instance;
            }
            catch (Exception e){
                LOGGER.error(e.getMessage());
            }
            return null;
        }

        public static JedisCluster getJedisCluster() {
            try{
                List<RedisHost> redisHostsConfig = getRedisNodesFromConfig();
                if (redisHostsConfig.size() == 1) {
                    RedisHost hostconf = redisHostsConfig.get(0);
                    //We have a single instance of Redis
                    LOGGER.error("RedisPool is only available for the Single instance of redis server");
                } else if (redisHostsConfig.size() > 1) {
                    //We have a cluster
                    Set<HostAndPort> hostAndPort = null;
                    for (RedisHost key : redisHostsConfig) {
                        hostAndPort.add(new HostAndPort(key.getHost(), key.getPort()));
                    }
                    clusterInstance = new JedisCluster(hostAndPort, poolConfig);
                }
                return clusterInstance;
            }
            catch (Exception e){
                LOGGER.error(e.getMessage());
            }
            return null;
        }


        private static JedisPoolConfig buildPoolConfig() {
            final JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(128);
            poolConfig.setMaxIdle(128);
            poolConfig.setMinIdle(16);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
            poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
            poolConfig.setNumTestsPerEvictionRun(3);
            poolConfig.setBlockWhenExhausted(true);
            return poolConfig;
        }
    }

}
