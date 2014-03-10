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
package org.springframework.data.neo4j.cross_store.support.node;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.cross_store.fieldaccess.JpaIdFieldAccessListenerFactory;
import org.springframework.data.neo4j.fieldaccess.ConvertingNodePropertyFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.FieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.FieldAccessorFactoryFactory;
import org.springframework.data.neo4j.fieldaccess.FieldAccessorListenerFactory;
import org.springframework.data.neo4j.fieldaccess.IndexingPropertyFieldAccessorListenerFactory;
import org.springframework.data.neo4j.fieldaccess.PropertyFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.QueryFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.ReadOnlyRelatedToCollectionFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelatedToCollectionFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelatedToSingleFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelatedToViaCollectionFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelatedToViaSingleFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.TraversalFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
* @author mh
* @since 30.04.12
*/
public class CrossStoreNodeDelegatingFieldAccessorFactory extends DelegatingFieldAccessorFactory {

    public CrossStoreNodeDelegatingFieldAccessorFactory(Neo4jTemplate template) {
        super(template);
    }

    @Override
    protected Collection<FieldAccessorListenerFactory> createListenerFactories() {
        return Arrays.asList(
                new IndexingPropertyFieldAccessorListenerFactory(
                        getTemplate(),
                        newPropertyFieldAccessorFactory(),
                        newConvertingNodePropertyFieldAccessorFactory()) {
                    @Override
                    public boolean accept(Neo4jPersistentProperty property) {
                        return property.findAnnotation(GraphProperty.class) != null && super.accept(property);
                    }
                },
                new JpaIdFieldAccessListenerFactory(template));
    }

    @Override
    protected Collection<? extends FieldAccessorFactory> createAccessorFactories() {
        return Arrays.asList(
                //new IdFieldAccessorFactory(),
                //new TransientFieldAccessorFactory(),
                new TraversalFieldAccessorFactory(template),
                new QueryFieldAccessorFactory(template),
                newPropertyFieldAccessorFactory(),
                newConvertingNodePropertyFieldAccessorFactory(),
                new RelatedToSingleFieldAccessorFactory(getTemplate()) {
                    @Override
                    public boolean accept(Neo4jPersistentProperty property) {
                        return property.findAnnotation(RelatedTo.class) != null && super.accept(property);
                    }
                },
                new RelatedToCollectionFieldAccessorFactory(getTemplate()),
                new ReadOnlyRelatedToCollectionFieldAccessorFactory(getTemplate()),
                new RelatedToViaSingleFieldAccessorFactory(getTemplate()),
                new RelatedToViaCollectionFieldAccessorFactory(getTemplate())
        );
    }

    private ConvertingNodePropertyFieldAccessorFactory newConvertingNodePropertyFieldAccessorFactory() {
        return new ConvertingNodePropertyFieldAccessorFactory(getTemplate()) {
            @Override
            public boolean accept(Neo4jPersistentProperty property) {
                return property.findAnnotation(GraphProperty.class) != null && super.accept(property);
            }
        };
    }

    private PropertyFieldAccessorFactory newPropertyFieldAccessorFactory() {
        return new PropertyFieldAccessorFactory(getTemplate()) {
            @Override
            public boolean accept(Neo4jPersistentProperty property) {
                return property.findAnnotation(GraphProperty.class) != null && super.accept(property);
            }
        };
    }

    public static class Factory extends FieldAccessorFactoryFactory {
        public DelegatingFieldAccessorFactory create(Neo4jTemplate template) {
            return new CrossStoreNodeDelegatingFieldAccessorFactory(template);
        }
    }
}
