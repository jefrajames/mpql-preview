/*
 * Copyright 2019 JF James.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jefrajames.mpql.preview.runner;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.util.AnnotationLiteral;
import lombok.extern.java.Log;
import org.eclipse.microprofile.graphql.GraphQLApi;

/**
 * This is a CDI extension that detects GraphQL components and generate a CDI
 * GraphQLConfig bean used by SchemaProducer.
 *
 * @author JF James
 */
@Log
public class GraphQLExtension implements Extension {

    private final Set<Bean<?>> graphQLComponents = new LinkedHashSet<>();

    // Add a GraphQLApi CDI bean 
    public void addGraphQLConfig(@Observes AfterBeanDiscovery abd) {
        
        log.log(Level.INFO, "AfterBeanDiscovery, {0} GraphQLApi classe(s) detected", graphQLComponents.size());
    
        abd
                .addBean()
                .types(GraphQLConfig.class)
                .qualifiers(new AnnotationLiteral<Any>() {}, new AnnotationLiteral<Default>() {})
                .scope(Dependent.class)
                .name(GraphQLConfig.class.getName())
                .beanClass(GraphQLConfig.class)
                .createWith(creationalContext -> {
                    GraphQLConfig instance = new GraphQLConfig();
                    instance.setGraphQLConfig(graphQLComponents);
                    return instance;
                });

    }

    // Detect and store GraphQLApi classes
    <X> void detectGraphQLComponents(@Observes ProcessBean<X> event) {
        if (event.getAnnotated().isAnnotationPresent(GraphQLApi.class)) {
            log.info("GraphQLApi class detected " + event.getBean().getBeanClass());
            graphQLComponents.add(event.getBean());
        }
    }

}
