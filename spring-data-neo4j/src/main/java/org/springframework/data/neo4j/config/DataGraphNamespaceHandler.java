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

/**
 * @author mh
 * @since 31.01.11
 */

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.neo4j.repository.CRUDRepository;
import org.springframework.data.neo4j.repository.GraphRepositoryFactoryBean;
import org.springframework.data.repository.config.*;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class DataGraphNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("config", new DataGraphBeanDefinitionParser());
        registerBeanDefinitionParser("repositories", new DataGraphRepositoryConfigDefinitionParser());
    }


    private static class DataGraphRepositoryConfigDefinitionParser extends AbstractRepositoryConfigDefinitionParser<DataGraphRepositoryConfigDefinitionParser.SimpleDataGraphRepositoryConfiguration, DataGraphRepositoryConfigDefinitionParser.DataGraphRepositoryConfiguration> {

        @Override
        protected SimpleDataGraphRepositoryConfiguration getGlobalRepositoryConfigInformation(Element element) {
            return new SimpleDataGraphRepositoryConfiguration(element);
        }

        @Override
        protected void postProcessBeanDefinition(DataGraphRepositoryConfiguration context, BeanDefinitionBuilder builder, BeanDefinitionRegistry registry, Object beanSource) {
            builder.addPropertyReference("neo4jTemplate", context.getNeo4jTemplateRef());
        }


        public interface DataGraphRepositoryConfiguration extends SingleRepositoryConfigInformation<SimpleDataGraphRepositoryConfiguration> {
            String NEO4J_TEMPLATE_REF = "neo4j-template-ref";
            String DEFAULT_NEO4J_TEMPLATE_REF = "neo4jTemplate";

            String getNeo4jTemplateRef();
        }

        public static class SimpleDataGraphRepositoryConfiguration extends RepositoryConfig<DataGraphRepositoryConfiguration, SimpleDataGraphRepositoryConfiguration> {

            protected SimpleDataGraphRepositoryConfiguration(Element repositoriesElement) {
                super(repositoriesElement, GraphRepositoryFactoryBean.class.getName());
            }

            @Override
            public String getNamedQueriesLocation() {
                return "classpath*:META-INF/neo4j-named-queries.properties";
            }

            @Override
            protected DataGraphRepositoryConfiguration createSingleRepositoryConfigInformationFor(Element element) {
                return new ManualDataGraphRepositoryConfiguration(element, this);
            }

            @Override
            public DataGraphRepositoryConfiguration getAutoconfigRepositoryInformation(String interfaceName) {
                return new AutomaticDataGraphRepositoryConfiguration(interfaceName, this);
            }

            @Override
            public Class<?> getRepositoryBaseInterface() {
                return CRUDRepository.class;
            }

            public String getNeo4jTemplateRef() {

                String contextRef = getSource().getAttribute(DataGraphRepositoryConfiguration.NEO4J_TEMPLATE_REF);
                return StringUtils.hasText(contextRef) ? contextRef : DataGraphRepositoryConfiguration.DEFAULT_NEO4J_TEMPLATE_REF;
            }

            private static class ManualDataGraphRepositoryConfiguration extends ManualRepositoryConfigInformation<SimpleDataGraphRepositoryConfiguration> implements DataGraphRepositoryConfiguration {

                public ManualDataGraphRepositoryConfiguration(Element element, SimpleDataGraphRepositoryConfiguration parent) {
                    super(element, parent);
                }

                @Override
                public String getNeo4jTemplateRef() {
                    return getAttribute(DataGraphRepositoryConfiguration.NEO4J_TEMPLATE_REF);
                }
            }

            private static class AutomaticDataGraphRepositoryConfiguration extends AutomaticRepositoryConfigInformation<SimpleDataGraphRepositoryConfiguration> implements DataGraphRepositoryConfiguration {

                public AutomaticDataGraphRepositoryConfiguration(String interfaceName, SimpleDataGraphRepositoryConfiguration parent) {
                    super(interfaceName, parent);
                }

                @Override
                public String getNeo4jTemplateRef() {
                    return getParent().getNeo4jTemplateRef();
                }
            }
        }

    }
}