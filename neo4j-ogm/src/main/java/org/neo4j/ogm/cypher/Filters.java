package org.neo4j.ogm.cypher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author: Vince Bickers
 */
public class Filters implements Iterable<Filter> {

    private List<Filter> filters = new ArrayList();

    public Filters add(Filter... filters) {
        for (Filter filter : filters) {
            this.filters.add(filter);
        }
        return this;
    }

    public Filters add(String key, Object value) {
        this.filters.add(new Filter(key, value));
        return this;
    }

    @Override
    public Iterator<Filter> iterator() {
        return filters.iterator();
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }
}
