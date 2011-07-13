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

package org.springframework.data.neo4j.support.relationship;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import javax.annotation.PostConstruct;

public class RelationshipEntityStateFactory {

	private GraphDatabaseContext graphDatabaseContext;
	
    private RelationshipEntityState.RelationshipStateDelegatingFieldAccessorFactory delegatingFieldAccessorFactory;

    public EntityState<RelationshipBacked, Relationship> getEntityState(final RelationshipBacked entity) {
		return new RelationshipEntityState<RelationshipBacked>(null,entity,entity.getClass(), graphDatabaseContext, delegatingFieldAccessorFactory);
	}

	public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
		this.graphDatabaseContext = graphDatabaseContext;
	}

    @PostConstruct
    private void setUp() {
       this.delegatingFieldAccessorFactory =  new RelationshipEntityState.RelationshipStateDelegatingFieldAccessorFactory(graphDatabaseContext);
    }

}
