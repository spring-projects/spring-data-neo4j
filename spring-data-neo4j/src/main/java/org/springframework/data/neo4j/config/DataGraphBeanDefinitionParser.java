/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.config;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.data.neo4j.support.GraphDatabaseServiceFactoryBean;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Element;

import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

public class DataGraphBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private static final String GRAPH_DATABASE_SERVICE = "graphDatabaseService";
    private static final String BASE_PACKAGE = "base-package";
    public static final String ASPECTJ_CONFIG = "org.springframework.data.neo4j.aspects.config.Neo4jAspectConfiguration";
    public static final String CROSS_STORE_CONFIG = "org.springframework.data.neo4j.cross_store.config.CrossStoreNeo4jConfiguration";

    
    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext context) {
        BeanDefinitionBuilder configBuilder = createConfigurationBeanDefinition(element);
        setupGraphDatabase(element, context, configBuilder);
        setupEntityManagerFactory(element, configBuilder);
        setupBaseEntities(element, configBuilder);
        setupConfigurationClassPostProcessor(context);
        return getSourcedBeanDefinition(configBuilder, element, context);
    }

    private void setupBaseEntities(Element element, BeanDefinitionBuilder configBuilder) {
        Set<String> initialEntityClasses = getInitialEntityClasses(element);
        if (initialEntityClasses!=null) {
            configBuilder.addPropertyValue("initialEntitySet", initialEntityClasses);
        }
    }


    public Set<String> getInitialEntityClasses(Element element) {

    		String basePackage = element.getAttribute(BASE_PACKAGE);

    		if (!hasText(basePackage)) {
    			return null;
    		}

            return BasePackageScanner.scanBasePackage(basePackage);
    	}


    private BeanDefinitionBuilder createConfigurationBeanDefinition(Element element) {
        BeanDefinitionBuilder configBuilder = createConfigBuilderByMode(element);
        configBuilder.setAutowireMode(Autowire.BY_TYPE.value());
        return configBuilder;
    }

    // todo cross-store
    private BeanDefinitionBuilder createConfigBuilderByMode(Element element) {
        if (isModeCrossStore(element)) return BeanDefinitionBuilder.rootBeanDefinition(CROSS_STORE_CONFIG);
        if (isModeAspectJ()) return BeanDefinitionBuilder.rootBeanDefinition(ASPECTJ_CONFIG);
        return BeanDefinitionBuilder.rootBeanDefinition(Neo4jConfiguration.class);
    }

    private boolean isModeCrossStore(Element element) {
        return isModeAspectJ() && isEntityManagerFactoryConfigured(element);
    }

    private boolean isModeAspectJ() {
        return ClassUtils.isPresent(ASPECTJ_CONFIG, getClass().getClassLoader());
    }

    private void setupConfigurationClassPostProcessor(final ParserContext parserContext) {
        BeanDefinitionRegistry beanDefinitionRegistry = parserContext.getRegistry();

        BeanDefinitionBuilder configurationClassPostProcessor = BeanDefinitionBuilder.rootBeanDefinition(ConfigurationClassPostProcessor.class);
        BeanNameGenerator beanNameGenerator = parserContext.getReaderContext().getReader().getBeanNameGenerator();
        AbstractBeanDefinition configurationClassPostProcessorBeanDefinition = configurationClassPostProcessor.getBeanDefinition();
        String beanName = beanNameGenerator.generateBeanName(configurationClassPostProcessorBeanDefinition, beanDefinitionRegistry);
        beanDefinitionRegistry.registerBeanDefinition(beanName, configurationClassPostProcessorBeanDefinition);
    }

    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    private void setupGraphDatabase(Element element, ParserContext context, BeanDefinitionBuilder configBuilder) {
        String graphDatabaseRef = element.getAttribute(GRAPH_DATABASE_SERVICE);
        if (!hasText(graphDatabaseRef)) {
            graphDatabaseRef = handleStoreDir(element, context, configBuilder);
        }
        configBuilder.addPropertyReference(GRAPH_DATABASE_SERVICE, graphDatabaseRef);
    }

    private void setupEntityManagerFactory(Element element, BeanDefinitionBuilder configBuilder) {
        if (isEntityManagerFactoryConfigured(element)) {
            String entityManagerFactory = element.getAttribute("entityManagerFactory");
            configBuilder.addPropertyReference("entityManagerFactory", entityManagerFactory);
        }
    }

    private boolean isEntityManagerFactoryConfigured(Element element) {
        String entityManagerFactory = element.getAttribute("entityManagerFactory");
        return hasText(entityManagerFactory);
    }

    private String handleStoreDir(Element element, ParserContext context, BeanDefinitionBuilder configBuilder) {
        String storeDir = element.getAttribute("storeDirectory");
        if (!hasText(storeDir)) return null;

        BeanDefinitionBuilder graphDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(GraphDatabaseServiceFactoryBean.class);
        graphDefinitionBuilder.addConstructorArgValue(storeDir);
        graphDefinitionBuilder.setScope("singleton");
        graphDefinitionBuilder.setDestroyMethodName("shutdown");
        context.getRegistry().registerBeanDefinition(GRAPH_DATABASE_SERVICE, graphDefinitionBuilder.getBeanDefinition());
        configBuilder.addPropertyReference(GRAPH_DATABASE_SERVICE, GRAPH_DATABASE_SERVICE);
        return GRAPH_DATABASE_SERVICE;
    }

    private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source, ParserContext context) {
        AbstractBeanDefinition definition = builder.getBeanDefinition();
        definition.setSource(context.extractSource(source));
        return definition;
    }




}
