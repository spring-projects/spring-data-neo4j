package org.neo4j.ogm.cypher.query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Vince Bickers
 */
public class SortOrder {

    private List<Ordering> orderings = new ArrayList();

    public SortOrder add(FilteringPagingAndSorting.Direction direction, String... properties) {
        orderings.add(new Ordering(direction, properties));
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!orderings.isEmpty()) {
            sb.append(" ORDER BY ");
            for (Ordering ordering : orderings) {
                sb.append(ordering);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(" ");
        }
        return sb.toString();
    }

    class Ordering {

        private final FilteringPagingAndSorting.Direction direction;
        private final String[] properties;

        public Ordering(FilteringPagingAndSorting.Direction direction, String... properties) {
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
                if (direction == FilteringPagingAndSorting.Direction.DESC) {
                    sb.append(" DESC");
                }
            }
            return sb.toString();
        }
    }

}


