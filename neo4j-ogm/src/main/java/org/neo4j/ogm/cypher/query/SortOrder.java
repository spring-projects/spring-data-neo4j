/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.neo4j.ogm.cypher.query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vince Bickers
 */
public class SortOrder {

    public static enum Direction {

        ASC, DESC
    }

    private List<SortClause> sortClauses = new ArrayList();

    public SortOrder add(Direction direction, String... properties) {
        sortClauses.add(new SortClause(direction, properties));
        return this;
    }

    public SortOrder add(String... properties) {
        sortClauses.add(new SortClause(Direction.ASC, properties));
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!sortClauses.isEmpty()) {
            sb.append(" ORDER BY ");
            for (SortClause ordering : sortClauses) {
                sb.append(ordering);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    class SortClause {

        private final Direction direction;
        private final String[] properties;

        public SortClause(Direction direction, String... properties) {
            this.direction = direction;
            this.properties = properties;
        }

        public String toString() {

            StringBuilder sb = new StringBuilder();

            if (properties.length > 0) {
                for (String n : properties) {
                    sb.append("$." + n);
                    if (direction == Direction.DESC) {
                        sb.append(" DESC");
                    }
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }
    }

}


