/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.infrastructure.core.config;

import javax.annotation.PostConstruct;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("/api/v1")
public class JerseyConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyConfig.class);
    private final ApplicationContext appCtx;

    @Autowired
    public JerseyConfig(ApplicationContext appCtx) {
        this.appCtx = appCtx;
        register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
        
        // Register components in constructor instead of PostConstruct
        registerComponents();
    }

    private void registerComponents() {
        // Register JAX-RS resources
        appCtx.getBeansWithAnnotation(Path.class).values()
            .forEach(component -> {
                LOG.debug("Registering JAX-RS resource: {}", component.getClass().getName());
                register(component.getClass());
            });

        // Register JAX-RS providers
        appCtx.getBeansWithAnnotation(Provider.class).values()
            .forEach(provider -> {
                LOG.debug("Registering JAX-RS provider: {}", provider.getClass().getName());
                register(provider);
            });
    }
}
