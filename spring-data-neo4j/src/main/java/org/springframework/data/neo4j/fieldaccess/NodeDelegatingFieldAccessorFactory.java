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

import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.util.Arrays;
import java.util.Collection;

/**
* @author Michael Hunger
* @since 30.09.2010
*/
public class NodeDelegatingFieldAccessorFactory extends DelegatingFieldAccessorFactory<NodeBacked> {
	
    public NodeDelegatingFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
        super(graphDatabaseContext);
    }

    @Override
    protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
        return Arrays.<FieldAccessorListenerFactory<?>>asList(
                new IndexingPropertyFieldAccessorListenerFactory(
                		graphDatabaseContext,
                		new PropertyFieldAccessorFactory(graphDatabaseContext.getConversionService()),
                		new ConvertingNodePropertyFieldAccessorFactory(graphDatabaseContext.getConversionService())),
                new ValidatingNodePropertyFieldAccessorListenerFactory(graphDatabaseContext)
        );
    }

    @Override
    protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
        return Arrays.<FieldAccessorFactory<?>>asList(
                new IdFieldAccessorFactory(),
                new TransientFieldAccessorFactory(),
                new TraversalFieldAccessorFactory(),
                new QueryFieldAccessorFactory(),
                new PropertyFieldAccessorFactory(graphDatabaseContext.getConversionService()),
                new ConvertingNodePropertyFieldAccessorFactory(graphDatabaseContext.getConversionService()),
                new SingleRelationshipFieldAccessorFactory(graphDatabaseContext),
                new OneToNRelationshipFieldAccessorFactory(graphDatabaseContext),
                new ReadOnlyOneToNRelationshipFieldAccessorFactory(graphDatabaseContext),
                new OneToNRelationshipEntityFieldAccessorFactory(graphDatabaseContext)
        );
    }
}
