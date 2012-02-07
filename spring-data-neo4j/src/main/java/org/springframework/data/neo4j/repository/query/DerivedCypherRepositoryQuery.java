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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

/**
 * {@link RepositoryQuery} implementation that derives a Cypher query from the {@link GraphQueryMethod}'s method name.
 * 
 * @author Oliver Gierke
 */
public class DerivedCypherRepositoryQuery extends CypherGraphRepositoryQuery {

    public static final Logger log = LoggerFactory.getLogger(DerivedCypherRepositoryQuery.class);
    private final CypherQueryDefinition query;

    /**
     * Creates a new {@link DerivedCypherRepositoryQuery} from the given {@link MappingContext},
     * {@link GraphQueryMethod} and {@link org.springframework.data.neo4j.support.Neo4jTemplate}.
     * 
     * @param mappingContext must not be {@literal null}.
     * @param queryMethod must not be {@literal null}.
     * @param template must not be {@literal null}.
     */
    public DerivedCypherRepositoryQuery(Neo4jMappingContext mappingContext, GraphQueryMethod queryMethod, Neo4jTemplate template) {
        super(queryMethod, template);
        Assert.notNull(mappingContext);

        EntityMetadata<?> info = queryMethod.getEntityInformation();
        PartTree tree = new PartTree(queryMethod.getName(), info.getJavaType());

        this.query = new CypherQueryCreator(tree, mappingContext, info.getJavaType()).createQuery();
        if (log.isDebugEnabled()) log.debug("Derived query: "+query+ "from method "+queryMethod);
    }

    @Override
    public Object resolveParameter(Object value, String parameterName, int index) {
        final Object newValue = super.resolveParameter(value, parameterName, index);
        PartInfo info = query.getPartInfo(index);
        if (info.isFullText()) {
            return String.format(QueryTemplates.PARAMETER_INDEX_QUERY,info.getIndexKey(),newValue);
        }
        return newValue;
    }

    /**
     * Returns the actual Cypher query applying {@link Pageable} or {@link Sort} instances.
     * 
     * @param accessor parameters
     * @return query string
     */
    protected String createQueryWithPagingAndSorting(ParameterAccessor accessor) {
        if (accessor.getPageable() != null) {
            return query.toString(accessor.getPageable());
        } else if (accessor.getSort() != null) {
            return query.toString(accessor.getSort());
        } else {
            return query.toString();
        }
    }
}
