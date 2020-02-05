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
package io.gravitee.resource.cache.ehcache;

import io.gravitee.resource.cache.Cache;
import io.gravitee.resource.cache.Element;
import net.sf.ehcache.Ehcache;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class EhCacheDelegate implements Cache {

    private final Ehcache ehcache;

    public EhCacheDelegate(Ehcache ehcache) {
        this.ehcache = ehcache;
    }

    @Override
    public String getName() {
        return ehcache.getName();
    }

    @Override
    public Object getNativeCache() {
        return ehcache;
    }

    @Override
    public Element get(Object key) {
        net.sf.ehcache.Element element = ehcache.get(key);
        return (element == null) ? null : (Element) element.getObjectValue();
    }

    @Override
    public void put(Element element) {
        ehcache.put(new net.sf.ehcache.Element(element.key(), element, 0, element.timeToLive()));
    }

    @Override
    public void evict(Object key) {
        ehcache.remove(key);
    }

    @Override
    public void clear() {
        ehcache.removeAll();
    }
}