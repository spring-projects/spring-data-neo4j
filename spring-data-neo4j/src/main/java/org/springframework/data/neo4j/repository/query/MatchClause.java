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

package org.springframework.data.neo4j.repository.query;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to build the {@code match} clause of a Cypher query.
 * 
 * @author Oliver Gierke
 */
class MatchClause {

    private final Iterable<Neo4jPersistentProperty> properties;

    /**
     * Creates a new {@link MatchClause} using the given
     * {@link org.springframework.data.neo4j.mapping.Neo4jMappingContext} and {@link PropertyPath}.
     * 
     * @param context must not be {@literal null}.
     * @param property must not be {@literal null}.
     */
    public MatchClause(Neo4jMappingContext context, PropertyPath property) {

        Assert.notNull(context);
        Assert.notNull(property);

        this.properties = context.getPersistentPropertyPath(property);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        String intermediate = null;

        for (Neo4jPersistentProperty property : properties) {

            if (!property.isRelationship()) {
                return intermediate;
            }

            RelationshipInfo info = property.getRelationshipInfo();
            Class<?> ownerType = property.getOwner().getType();

            intermediate = intermediate == null ? asVariableReference(StringUtils.uncapitalize(ownerType
                    .getSimpleName())) : intermediate;
            intermediate = String.format(getPattern(info), intermediate, info.getType(),
                    asVariableReference(property.getName()));
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