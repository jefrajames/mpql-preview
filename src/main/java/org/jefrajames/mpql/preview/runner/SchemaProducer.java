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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.logging.Level;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import lombok.extern.java.Log;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

/**
 * This class generates the GraphQL schema from GraphQLConfig previously
 * initialized by GraphQLExtension.
 *
 * The schema is eagerly generated by observing
 *
 * @Initialized(ApplicationScoped.class) CDI event.
 *
 * @author JF James
 */
@Log
@ApplicationScoped
public class SchemaProducer {

    @Inject
    private BeanManager beanManager;

    private void analyzeGraphQLClass(Class targetClass) {

        log.info("Analysing GraphQLApi class " + targetClass);
        Method allMethods[] = targetClass.getDeclaredMethods();
        for (int i = 0; i < allMethods.length; i++) {
            Method m = allMethods[i];
            log.info("method found " + m);
            // How to detect annotations method?
            Annotation[] methodAnnotations = m.getAnnotations();
            for (int j = 0; j < methodAnnotations.length; j++) {
                Annotation methodAnnotation = methodAnnotations[j];
                log.info("\t method annotation found " + methodAnnotations);
                if (methodAnnotation instanceof Query) {
                    log.info("\t @Query detected with description " + ((Query) methodAnnotation).description());
                }
                if (methodAnnotation instanceof Mutation) {
                    log.info("\t @Mutation detected with description " + ((Mutation) methodAnnotation).description());
                }
            }
            // How to detect method arguments?
            Parameter[] methodParams = m.getParameters();
            for (int k = 0; k < methodParams.length; k++) {
                Parameter param = methodParams[k];
                log.info("\t\t parameter found " + param);
                // How to detect annotation arguments?
                Annotation[] paramAnnotations = param.getAnnotations();
                for (int l = 0; l < paramAnnotations.length; l++) {
                    Annotation paramAnnotation = paramAnnotations[l];
                    log.info("\t\t\t parameter annotation found " + paramAnnotation);
                    if (paramAnnotation instanceof Argument) {
                        String value = ((Argument) paramAnnotation).value();
                        String description = ((Argument) paramAnnotation).description();
                        log.info("\t\t\t @Argument detected " + paramAnnotation);
                    }
                }

            }

        }

    }

    // This CDI bean is dynamically generated by GraphQLExtension
    @Inject
    private GraphQLConfig graphQLConfig;

    private GraphQLSchema schema;

    private void generateSchemaFromConfig() {

        if (graphQLConfig.getGraphQLComponents().size() == 0) {
            throw new IllegalStateException("No GraphQLApi class detected, check your application!");
        }

        GraphQLSchemaGenerator schemaGen = new GraphQLSchemaGenerator()
                .withResolverBuilders(new AnnotatedResolverBuilder())
                .withValueMapperFactory(new JacksonValueMapperFactory());

        for (Bean<?> bean : graphQLConfig.getGraphQLComponents()) {
            schemaGen.withOperationsFromSingleton(
                    beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)),
                    bean.getBeanClass());
            analyzeGraphQLClass(bean.getBeanClass());
        }

        schema = schemaGen.generate();
    }

    @PostConstruct
    private void createSchema() {
        generateSchemaFromConfig();
    }

    @Produces
    public GraphQLSchema getSchema() {
        return schema;
    }

    // Enables to build the schema just after the application context is initialized
    // See CDI 2.0 Spec 6.7.3
    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.log(Level.INFO, "GraphQL schema ready, with {0} GraphQLApi classe(s)", graphQLConfig.getGraphQLComponents().size());
    }

}
