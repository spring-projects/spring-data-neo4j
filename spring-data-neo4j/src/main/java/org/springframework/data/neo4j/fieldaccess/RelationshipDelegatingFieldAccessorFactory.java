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
package org.springframework.data.neo4j.fieldaccess;

import java.util.Arrays;
import java.util.Collection;



import org.springframework.data.neo4j.support.Neo4jTemplate;

public class RelationshipDelegatingFieldAccessorFactory extends DelegatingFieldAccessorFactory {
    public RelationshipDelegatingFieldAccessorFactory(Neo4jTemplate template) {
        super(template);
    }

    @Override
    protected Collection<FieldAccessorListenerFactory> createListenerFactories() {
        return Arrays.<FieldAccessorListenerFactory>asList(
                new IndexingPropertyFieldAccessorListenerFactory(
                        template,
                        new PropertyFieldAccessorFactory(template),
                        new ConvertingNodePropertyFieldAccessorFactory(template)),
                new ValidatingPropertyFieldAccessorListenerFactory(template));
    }

    @Override
    protected Collection<? extends FieldAccessorFactory> createAccessorFactories() {
        return Arrays.<FieldAccessorFactory>asList(
                new TransientFieldAccessorFactory(),
                new IdFieldAccessorFactory(template),
                new RelationshipNodeFieldAccessorFactory(template),
                new PropertyFieldAccessorFactory(template),
                new ConvertingNodePropertyFieldAccessorFactory(template),
                new DynamicPropertiesFieldAccessorFactory(template)
        );
    }

    public static class Factory extends FieldAccessorFactoryFactory {
        @Override
        public DelegatingFieldAccessorFactory create(Neo4jTemplate template) {
            return new RelationshipDelegatingFieldAccessorFactory(template);
        }
    }
}