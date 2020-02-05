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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisHost {
    private String Host;
    private Integer Port;
    private final Logger LOGGER = LoggerFactory.getLogger(RedisHost.class);

    public RedisHost(String host, Integer port ){
        this.Host = host;
        this.Port = port;
    }

    public RedisHost(){}

    public String getHost() {
        return Host;
    }

    public Integer getPort() {
        return Port;
    }

    public void setHost(String host) {
        Host = host;
    }

    public void setPort(Integer port) {
        Port = port;
    }

    @Override
    public String toString() {
        return Host + ":" + Port;
    }
}
