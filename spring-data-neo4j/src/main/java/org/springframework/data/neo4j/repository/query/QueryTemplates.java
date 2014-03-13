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

import java.util.Locale;

/**
 * String templates to build Cypher queries.
 * 
 * @author Oliver Gierke
 */
public abstract class QueryTemplates {

    public static final String PARAMETER = "%d";

    public static final String PARAMETER_INDEX_QUERY = "%s:%s";
    public static final String PARAMETER_INDEX_QUERY_STARTS_WITH = "%s:%s*";
    public static final String PARAMETER_INDEX_QUERY_CONTAINS = "%s:*%s*";
    public static final String PARAMETER_INDEX_QUERY_ENDSWITH = "%s:*%s";
    public static final String PARAMETER_INDEX_QUERY_QUOTED = "%s:\"%s\"";


    public static final String VARIABLE = "`%s`";

    public static final String PLACEHOLDER = String.format("{%s}", PARAMETER);
    private static final String DIRECTION_INCOMING = "<-[:`%s`]-";
    private static final String DIRECTION_OUTGOING = "-[:`%s`]->";
    private static final String DIRECTION_BOTH = "-[:`%s`]-";
    static final String MATCH_CLAUSE_SINGLE = "(`%s`)";
    static final String MATCH_CLAUSE = "(`%s`)%s(`%s`)";
    static final String MATCH_CLAUSE2 = "%s%s(`%s`)";

    static final String DEFAULT_INDEXBASED_START_CLAUSE = "`%s`=node:__types__(className=\"%s\")";
    static final String DEFAULT_LABELBASED_MATCH_START_CLAUSE = "(`%s`%s)";

    public static final String START_NODE_LOOKUP = "`%s`=node({%d})";
    static final String SKIP_LIMIT = " SKIP %d LIMIT %d";
    static final String START_CLAUSE_INDEX_LOOKUP = "`%s`=node:`%s`(`%s`=" + PLACEHOLDER + ")";
    static final String START_CLAUSE_INDEX_QUERY = "`%s`=node:`%s`(" + PLACEHOLDER + ")";
    static final String WHERE_CLAUSE_1 = "`%1$s`.`%2$s` %3$s {%4$d}";
    static final String WHERE_CLAUSE_ID = "id(`%1$s`) %2$s {%3$d}";
    static final String INDEXBASED_WHERE_TYPE_CHECK = "`%1$s`.__type__ IN [%2$s]";
    static final String LABELBASED_WHERE_TYPE_CHECK = "`%1$s`:%2$s";
    static final String WHERE_CLAUSE_0 = "`%1$s`.`%2$s` %3$s ";
    static final String SORT_CLAUSE = "%s %s";
    static final String ORDER_BY_CLAUSE = " ORDER BY %s";
    public static final String REGEX_WILDCARD = ".*";
    public static final String LUCENE_WILDCARD = "*";


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

    public static String formatIndexQuery(PartInfo partInfo, Object value) {
        value = formatIndexValue(partInfo,value);
        return String.format(QueryTemplates.PARAMETER_INDEX_QUERY, partInfo.getIndexKey(), value);
    }

    private static Object formatIndexValue(PartInfo partInfo, Object value) {
        if (value==null) return null;
        switch (partInfo.getType()) {
            case CONTAINING: checkWhiteSpace(partInfo,value); return append(prepend(value, LUCENE_WILDCARD), LUCENE_WILDCARD);
            case STARTING_WITH: checkWhiteSpace(partInfo,value); return append(value, LUCENE_WILDCARD);
            case ENDING_WITH: checkWhiteSpace(partInfo,value); return prepend(value, LUCENE_WILDCARD);
            default : if (containsWhiteSpace(value)) return "\""+value+"\"";
        }
        return value;
    }

    private static boolean containsWhiteSpace(Object value) {
        return value instanceof String && value.toString().contains(" ");
    }

    private static String prepend(Object value, String prefix) {
        String stringValue = value.toString();
        if (stringValue.startsWith(prefix)) return stringValue;
        return prefix+stringValue;
    }

    private static void checkWhiteSpace(PartInfo partInfo,Object value) {
        if (containsWhiteSpace(value)) throw new RepositoryQueryException("Value for "+partInfo.getLeafProperty()+" contains whitespace. It cannot be used with wildcards "+value);
    }

    private static String append(Object value, String suffix) {
        String stringValue = value.toString();
        if (stringValue.endsWith(suffix)) return stringValue;
        return stringValue+ suffix;
    }

    public static Object formatExpression(PartInfo partInfo, Object value) {
        if (value==null) return null;
        switch (partInfo.getType()) {
            case CONTAINING: return append(prepend(value, REGEX_WILDCARD), REGEX_WILDCARD);
            case STARTING_WITH: return append(prepend(value, "^"),REGEX_WILDCARD);
            case ENDING_WITH: return append(prepend(value,REGEX_WILDCARD), "$");
        }
        return value;
    }
}
