package org.neo4j.ogm.cypher.query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Vince Bickers
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
                sb.append("$." + ordering);
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
                    sb.append(n);
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                if (direction == Direction.DESC) {
                    sb.append(" DESC");
                }
            }
            return sb.toString();
        }
    }

}


