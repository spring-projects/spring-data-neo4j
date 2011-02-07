package org.springframework.data.graph.neo4j.config;

/**
 * @author mh
 * @since 31.01.11
 */

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class DataGraphNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("config", new DataGraphBeanDefinitionParser());
    }
}