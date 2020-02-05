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
package io.gravitee.resource.cache.configuration;

import io.gravitee.resource.api.ResourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class CacheResourceConfiguration implements ResourceConfiguration {

    private String name = "my-cache";
    private long timeToIdleSeconds = 0;
    private long timeToLiveSeconds = 0;
    private long maxEntriesLocalHeap = 1000;
    private CacheType cacheType = CacheType.Redis;
    private String redisHostAndPort = null;
    private int redisDatabase = 0;
    private final Logger LOGGER = LoggerFactory.getLogger(CacheResourceConfiguration.class);


    public CacheResourceConfiguration(){

    }

    public long getMaxEntriesLocalHeap() {
        return maxEntriesLocalHeap;
    }

    public void setMaxEntriesLocalHeap(long maxEntriesLocalHeap) {
        this.maxEntriesLocalHeap = maxEntriesLocalHeap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    public void setTimeToIdleSeconds(long timeToIdleSeconds) {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    public long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(long timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public void setCacheType(CacheType cacheType) {
        this.cacheType = cacheType;
    }

    public int getRedisDatabase(){ return this.redisDatabase; }

    public void setRedisDatabase(int database){this.redisDatabase = database;}

    public String getRedisHostAndPort(){return this.redisHostAndPort;}

    public void setRedisHostAndPort(String redisHostAndPort) { this.redisHostAndPort = redisHostAndPort; }

    private List<RedisHost> getRedisHostsFromUri(String hostAndPortString){
        List<RedisHost> hostAndPortList = new ArrayList<RedisHost>();
        try{
            String[] serverArray = hostAndPortString.split(",");
            if(serverArray.length > 0){
                for (String ipPort : serverArray) {
                    String[] ipPortPair = ipPort.split(":");
                    if(ipPortPair.length == 2){
                        //hostAndPortHash.put(ipPortPair[0].trim(), Integer.valueOf(ipPortPair[1].trim()));
                        hostAndPortList.add(new RedisHost(ipPortPair[0].trim(), Integer.valueOf(ipPortPair[1].trim())));
                    }
                }
            }
        }
        catch (Exception e){
            LOGGER.error(e.getMessage());
        }
        return hostAndPortList;
    }

    public List<RedisHost> getRedisNodes(Environment environment){
        List<RedisHost> hostAndPortList = new ArrayList<RedisHost>();
        try{
            //First Try override values..
            String hostAndPortString = this.getRedisHostAndPort();
            if(hostAndPortString != null && !hostAndPortString.isEmpty()){
                hostAndPortList = getRedisHostsFromUri(hostAndPortString);
            }
            else{
                //try to get it from the gravitee.yml
                String nodesConfigString = environment.getProperty("cache.uri", "");
                if(nodesConfigString != null && !nodesConfigString.isEmpty()){
                    hostAndPortList = getRedisHostsFromUri(nodesConfigString);
                    LOGGER.debug("Length of my list is :{}", hostAndPortList.size());
                }
            }
        }
        catch (Exception e){
            LOGGER.error(e.getMessage());
        }
        return hostAndPortList;
    }

    public Boolean isRedisCluster(){
        Boolean result = false;
        String hostAndPortString = this.getRedisHostAndPort();
        if(hostAndPortString != null){
            String[] serverArray = hostAndPortString.split(",");
            if(serverArray.length > 1){
                result = true;
            }
        }
        return result;
    }
}
