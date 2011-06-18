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

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.graph.neo4j.mapping.Neo4JMappingContext;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to create {@link CypherQuery} instances.
 *
 * @author Oliver Gierke
 */
public class CypherQuery {

    private final String TEMPLATE = "start %s match % where %";

    private final Neo4JMappingContext context;
    private final TypeInformation<?> rootType;

    private final Set<MatchClause> matchClauses;

    public CypherQuery(TypeInformation<?> rootType, Neo4JMappingContext context) {

        Assert.notNull(rootType);
        Assert.notNull(context);

        this.rootType =  rootType;
        this.context = context;
        this.matchClauses = new HashSet<MatchClause>();
    }

    public CypherQuery addRestriction(Part part) {

        matchClauses.add(new MatchClause(context, part.getProperty()));
        return this;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(TEMPLATE, "TODO", StringUtils.collectionToCommaDelimitedString(matchClauses), "TODO");
    }
}
