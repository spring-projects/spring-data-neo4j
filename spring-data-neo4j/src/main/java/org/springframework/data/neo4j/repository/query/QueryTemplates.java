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

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.mapping.RelationshipInfo;

/**
 * String templates to build Cypher queries.
 * 
 * @author Oliver Gierke
 */
public abstract class QueryTemplates {

    public static final String PARAMETER = "%d";
    public static final String PARAMETER_INDEX_QUERY = "%s:%s";
    public static final String VARIABLE = "`%s`";

    public static final String PLACEHOLDER = String.format("{%s}", PARAMETER);
    private static final String DIRECTION_INCOMING = "<-[:`%s`]-";
    private static final String DIRECTION_OUTGOING = "-[:`%s`]->";
    private static final String DIRECTION_BOTH = "-[:`%s`]-";
    static final String MATCH_CLAUSE = "`%s`%s`%s`";
    static final String MATCH_CLAUSE2 = "%s%s`%s`";

    static final String DEFAULT_START_CLAUSE = "`%s`=node:__types__(className=\"%s\")";
    static final String SKIP_LIMIT = " skip %d limit %d";
    static final String START_CLAUSE = "`%s`=node:`%s`(`%s`=" + PLACEHOLDER + ")";
    static final String START_CLAUSE_FULLTEXT = "`%s`=node:`%s`(" + PLACEHOLDER + ")";
    static final String WHERE_CLAUSE_1 = "`%1$s`.`%2$s`! %3$s {%4$d}";
    static final String WHERE_CLAUSE_0 = "`%1$s`.`%2$s`! %3$s ";
    static final String SORT_CLAUSE = "%s %s";
    static final String ORDER_BY_CLAUSE = " order by %s";


    static String getArrow(RelationshipInfo info) {
        return String.format(getTemplate(info.getDirection()), info.getType());
    }

    private static String getTemplate(Direction direction) {

        switch (direction) {
        case OUTGOING:
            return DIRECTION_OUTGOING;
        case INCOMING:
            return DIRECTION_INCOMING;
        case BOTH:
            return DIRECTION_BOTH;
        default:
            throw new IllegalArgumentException("Unsupported direction!");
        }
    }

    private QueryTemplates() {

    }
}
