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

package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

public class NoopRelationshipTypeRepresentationStrategy implements RelationshipTypeRepresentationStrategy {

    @Override
    public void writeTypeTo(Relationship state, StoredEntityType type) {
    }

    @Override
    public <U> ClosableIterable<Relationship> findAll(StoredEntityType type) {
        throw new UnsupportedOperationException("findAll not supported.");
    }

    @Override
    public long count(StoredEntityType type) {
        throw new UnsupportedOperationException("count not supported.");
    }

    @Override
    public void preEntityRemoval(Relationship state) {
    }

    @Override
    public boolean isLabelBased() {
        return false;
    }

    @Override
    public Object readAliasFrom(Relationship state) {
        return null;
    }
}
