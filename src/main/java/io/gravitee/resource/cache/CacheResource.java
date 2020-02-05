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
package io.gravitee.resource.cache;

import io.gravitee.resource.api.AbstractConfigurableResource;
import io.gravitee.resource.cache.configuration.CacheResourceConfiguration;
import io.gravitee.resource.cache.configuration.CacheType;
import io.gravitee.resource.cache.ehcache.EhCacheDelegate;
import io.gravitee.resource.cache.redis.RedisDelegate;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class CacheResource extends AbstractConfigurableResource<CacheResourceConfiguration> implements ApplicationContextAware  {

    private final Logger LOGGER = LoggerFactory.getLogger(CacheResource.class);

    private CacheManager cacheManager;
    private Cache cache;
    private CacheType cacheType;
    private Environment environment;

   /* public CacheResource(){
        CacheType cacheType = configuration().getCacheType();
        this.cacheType = cacheType;
        LOGGER.info("Cache type is: {}", this.cacheType);
    }*/

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        CacheType cacheType = configuration().getCacheType();
        this.cacheType = cacheType;
        LOGGER.info("Cache type is: {}", this.cacheType);

        if(this.cacheType.equals(CacheType.Ehcache)){
            Configuration configuration = new Configuration();
            configuration.setName(configuration().getName());
            cacheManager = new CacheManager(configuration);

            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setEternal(false);
            cacheConfiguration.setTimeToIdleSeconds(configuration().getTimeToIdleSeconds());
            cacheConfiguration.setTimeToLiveSeconds(configuration().getTimeToLiveSeconds());
            cacheConfiguration.setMaxEntriesLocalHeap(configuration().getMaxEntriesLocalHeap());
            cacheConfiguration.setName(configuration().getName());

            LOGGER.info("Create a new cache: {}", configuration().getName());
            net.sf.ehcache.Cache ehCache = new net.sf.ehcache.Cache(cacheConfiguration);
            cache = new EhCacheDelegate(ehCache);
            cacheManager.addCache(ehCache);
        }
        else if(this.cacheType.equals(CacheType.Redis)){
            cache = new RedisDelegate(configuration(), environment);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (cacheManager != null) {
            LOGGER.info("Clear cache {}", configuration().getName());
            cacheManager.shutdown();
        }
    }



    public Cache getCache() {
        return this.cache;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        LOGGER.debug("In setApplicationContext method");
        Environment env = applicationContext.getEnvironment();
        //String redisconf = env.getProperty("cache.uri", "false");
        //LOGGER.debug("redisconf is: {}", redisconf);
        this.environment = env;
    }
}
