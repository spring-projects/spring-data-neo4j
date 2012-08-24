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

import java.util.Iterator;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

/**
 * {@link AbstractQueryCreator} implementation to build {@link CypherQueryDefinition}s.
 * 
 * @author Oliver Gierke
 */
class CypherQueryCreator extends AbstractQueryCreator<CypherQueryDefinition, CypherQueryBuilder> {

    private final MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context;
    private final Class<?> domainClass;

    /**
     * Creates a new {@link CypherQueryCreator} using the given {@link PartTree}, {@link org.springframework.data.neo4j.support.mapping.Neo4jMappingContext} and domain
     * class.
     * 
     * @param tree must not be {@literal null}.
     * @param context must not be {@literal null}.
     * @param domainClass must not be {@literal null}.
     */
    public CypherQueryCreator(PartTree tree, MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context, Class<?> domainClass) {

        super(tree);

        Assert.notNull(context);
        Assert.notNull(domainClass);

        this.context = context;
        this.domainClass = domainClass;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#create(org.springframework.data.repository.query.parser.Part, java.util.Iterator)
     */
    @Override
    protected CypherQueryBuilder create(Part part, Iterator<Object> iterator) {

        CypherQueryBuilder builder = new CypherQueryBuilder(context, domainClass);
        builder.addRestriction(part);

        return builder;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#and(org.springframework.data.repository.query.parser.Part, java.lang.Object, java.util.Iterator)
     */
    @Override
    protected CypherQueryBuilder and(Part part, CypherQueryBuilder builder, Iterator<Object> iterator) {
        return builder.addRestriction(part);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#or(java.lang.Object, java.lang.Object)
     */
    @Override
    protected CypherQueryBuilder or(CypherQueryBuilder base, CypherQueryBuilder builder) {
        throw new UnsupportedOperationException("Or is not supported currently!");
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#complete(java.lang.Object, org.springframework.data.domain.Sort)
     */
    @Override
    protected CypherQueryDefinition complete(CypherQueryBuilder builder, Sort sort) {
        return builder.buildQuery(sort);
    }
}
