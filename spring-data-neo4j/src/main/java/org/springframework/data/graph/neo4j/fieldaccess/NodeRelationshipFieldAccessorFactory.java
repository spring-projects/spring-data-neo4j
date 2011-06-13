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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public abstract class NodeRelationshipFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {

    protected GraphDatabaseContext graphDatabaseContext;
    
    public NodeRelationshipFieldAccessorFactory(
			GraphDatabaseContext graphDatabaseContext) {
		super();
		this.graphDatabaseContext = graphDatabaseContext;
	}

	@SuppressWarnings({"unchecked"})
    protected Class<? extends NodeBacked> targetFrom(Field field, RelatedTo relatedTo) {
        if (relatedTo!=null && relatedTo.elementClass()!=NodeBacked.class) return relatedTo.elementClass();
        if (Iterable.class.isAssignableFrom(field.getType())) {
            return (Class<? extends NodeBacked>) GenericCollectionTypeResolver.getCollectionFieldType(field);
        }
        return (Class<? extends NodeBacked>) field.getType();
    }

    protected Direction dirFrom(RelatedTo relAnnotation) {
        return relAnnotation.direction().toNeo4jDir();
    }

    protected DynamicRelationshipType typeFrom(Field field) {
        return DynamicRelationshipType.withName(DelegatingFieldAccessorFactory.getNeo4jPropertyName(field));
    }

    protected DynamicRelationshipType typeFrom(RelatedTo relAnnotation) {
        return DynamicRelationshipType.withName(relAnnotation.type());
    }

    protected DynamicRelationshipType typeFrom(Field field, RelatedTo relAnnotation) {
        return "".equals(relAnnotation.type()) ? typeFrom(field) : typeFrom(relAnnotation);
    }

    protected RelatedTo getRelationshipAnnotation(Field field) {
        return field.getAnnotation(RelatedTo.class);
    }

    protected boolean hasValidRelationshipAnnotation(Field field) {
        final RelatedTo relAnnotation = getRelationshipAnnotation(field);
        return (relAnnotation != null);
    }
}
