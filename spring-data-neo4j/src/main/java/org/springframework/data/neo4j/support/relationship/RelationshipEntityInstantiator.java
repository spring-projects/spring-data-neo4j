/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.support.relationship;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.support.mapping.AbstractConstructorEntityInstantiator;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import sun.reflect.ReflectionFactory;

/**
 * Instantiator for relationship entities uses non constructor invoking {@link ReflectionFactory} internal to sun reflect
 * package.
 * Part of the SPI, not intended for public use.
 */

public class RelationshipEntityInstantiator extends AbstractConstructorEntityInstantiator<Relationship> {

    private final EntityStateHandler entityStateHandler;

    public RelationshipEntityInstantiator(EntityStateHandler entityStateHandler) {
        this.entityStateHandler = entityStateHandler;
    }

    @Override
    protected void setState(Object entity, Relationship relationship) {
        this.entityStateHandler.setPersistentState(entity,relationship);
    }

    @Override
    protected String getFailingMessageForClass(Class<?> entityClass, Class<Relationship> relationshipClass) {
        return entityClass.getSimpleName() + ": entity must have a no-arg constructor.";
    }


    /*
    protected <T> StateBackedCreator<T, Relationship> createInstantiator(Class<T> type, final Class<Relationship> stateType) {
        StateBackedCreator<T,Relationship> creator = createWithoutConstructorInvocation(type,stateType);
        if (creator !=null) return creator;
        return createFailingInstantiator(stateType);
    }
    */

	@Override
	protected Class<Relationship> getStateInterface() { return Relationship.class; }

}
