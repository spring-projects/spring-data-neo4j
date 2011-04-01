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

package org.springframework.data.graph.neo4j.support.relationship;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.persistence.AbstractConstructorEntityInstantiator;
import org.springframework.data.persistence.EntityInstantiator;

import org.springframework.data.persistence.StateBackedCreator;
import sun.reflect.ReflectionFactory;

/**
 * Instantiator for relationship entities uses non constructor invoking {@link ReflectionFactory} internal to sun reflect
 * package.
 * Part of the SPI, not intended for public use.
 */

public class ConstructorBypassingGraphRelationshipInstantiator extends AbstractConstructorEntityInstantiator<RelationshipBacked, Relationship> {

    @Override
    protected void setState(RelationshipBacked entity, Relationship relationship) {
        entity.setPersistentState(relationship);
    }
    /*
    protected <T extends RelationshipBacked> StateBackedCreator<T, Relationship> createInstantiator(Class<T> type, final Class<Relationship> stateType) {
        StateBackedCreator<T,Relationship> creator = createWithoutConstructorInvocation(type,stateType);
        if (creator !=null) return creator;
        return createFailingInstantiator(stateType);
    }
    */
}
