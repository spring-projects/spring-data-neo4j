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

package org.springframework.data.graph.neo4j.repository.query;

import org.springframework.data.graph.neo4j.mapping.Neo4JMappingContext;
import org.springframework.data.graph.neo4j.mapping.Neo4JPersistentProperty;
import org.springframework.data.graph.neo4j.mapping.RelationshipInfo;
import org.springframework.data.repository.query.parser.Property;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to build the {@code match} clause of a Cypher query.
 * 
 * @author Oliver Gierke
 */
class MatchClause {

    private final Iterable<Neo4JPersistentProperty> properties;

    /**
     * Creates a new {@link MatchClause} using the given {@link Neo4JMappingContext} and {@link Property}.
     * 
     * @param context must not be {@literal null}.
     * @param property must not be {@literal null}.
     */
    public MatchClause(Neo4JMappingContext context, Property property) {

        Assert.notNull(context);
        Assert.notNull(property);

        Class<?> rootType = property.getOwningType().getType();
        this.properties = context.getPersistentPropertyPath(rootType, property.toDotPath());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        String intermediate = null;

        for (Neo4JPersistentProperty property : properties) {

            if (!property.isRelationship()) {
                return intermediate;
            }

            RelationshipInfo info = property.getRelationshipInfo();
            Class<?> ownerType = property.getOwner().getType();

            intermediate = intermediate == null ? asVariableReference(StringUtils.uncapitalize(ownerType.getSimpleName()))
                    : intermediate;
            intermediate = String.format(getPattern(info), intermediate, info.getType(), asVariableReference(property.getName()));
        }

        return intermediate.toString();
    }

    /**
     * Returns the given value as variable reference.
     * 
     * @param value
     * @return
     */
    private static String asVariableReference(String value) {
        return String.format("(%s)", value);
    }

    /**
     * Returns the clause pattern for the given {@link RelationshipInfo}.
     * 
     * @param info must not be {@literal null}.
     * @return
     */
    private static String getPattern(RelationshipInfo info) {

        switch (info.getDirection()) {
        case OUTGOING:
            return "%s-[:%s]->%s";
        case INCOMING:
            return "%s<-[:%s]-%s";
        case BOTH:
            return "%s-[:%s]-%s";
        default:
            throw new IllegalArgumentException("Unsupported direction!");
        }
    }
}