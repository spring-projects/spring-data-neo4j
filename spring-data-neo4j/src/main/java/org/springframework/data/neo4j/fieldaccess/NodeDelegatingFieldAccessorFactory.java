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


import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Arrays;
import java.util.Collection;

/**
* @author Michael Hunger
* @since 30.09.2010
*/
public class NodeDelegatingFieldAccessorFactory extends DelegatingFieldAccessorFactory {
	
    public NodeDelegatingFieldAccessorFactory(Neo4jTemplate template) {
        super(template);
    }

    @Override
    protected Collection<FieldAccessorListenerFactory> createListenerFactories() {
        return Arrays.<FieldAccessorListenerFactory>asList(
                new IndexingPropertyFieldAccessorListenerFactory(
                        template,
                		new PropertyFieldAccessorFactory(template),
                		new ConvertingNodePropertyFieldAccessorFactory(template)),
                new ValidatingPropertyFieldAccessorListenerFactory(template)
        );
    }

    @Override
    protected Collection<? extends FieldAccessorFactory> createAccessorFactories() {
        return Arrays.<FieldAccessorFactory>asList(
                new IdFieldAccessorFactory(template),
                new TransientFieldAccessorFactory(),
                new LabelFieldAccessorFactory(template),
                new TraversalFieldAccessorFactory(template),
                new QueryFieldAccessorFactory(template),
                new PropertyFieldAccessorFactory(template),
                new ConvertingNodePropertyFieldAccessorFactory(template),
                new RelatedToSingleFieldAccessorFactory(template),
                new RelatedToCollectionFieldAccessorFactory(template),
                new ReadOnlyRelatedToCollectionFieldAccessorFactory(template),
                new RelatedToViaCollectionFieldAccessorFactory(template),
                new RelatedToViaSingleFieldAccessorFactory(template),
                new DynamicPropertiesFieldAccessorFactory(template)
        );
    }

    public static class Factory extends FieldAccessorFactoryFactory {
        @Override
        public DelegatingFieldAccessorFactory create(Neo4jTemplate template) {
            return new NodeDelegatingFieldAccessorFactory(template);
        }
    }
}
