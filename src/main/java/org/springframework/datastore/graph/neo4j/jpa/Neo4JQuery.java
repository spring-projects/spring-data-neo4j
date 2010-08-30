package org.springframework.datastore.graph.neo4j.jpa;

import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.finder.Finder;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;

import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author Michael Hunger
* @since 29.08.2010
*/
public class Neo4JQuery implements Query {
    protected final Finder<? extends NodeBacked> finder;
    protected final Class<? extends NodeBacked> entityClass;
    protected final String qlString;
    private final PersistenceUnitInfo info;
    private final Pattern fromPattern = Pattern.compile("^.*\\bfrom\\s+([A-Z][A-Za-z0-9]+)\\b.*");
    private int startPosition=0;
    private int maxResult=-1;
    private QueryExectuor queryExectuor;

    public Neo4JQuery(final String qlString, final FinderFactory finderFactory, final PersistenceUnitInfo info) {
        this.qlString = qlString;
        this.info = info;
        final Matcher matcher = fromPattern.matcher(qlString);
        if (matcher.matches()) {
            final String shortName = matcher.group(1);
            entityClass=getEntityClass(shortName);
            finder = finderFactory.getFinderForClass(entityClass);
            queryExectuor = createExecutor(qlString);
        } else {
            throw new IllegalAccessError("Unable to parse query "+qlString);
        }
    }

    private QueryExectuor createExecutor(String qlString) {
        if (qlString.contains(" count(")) return new QueryExectuor() {
            @Override
            protected Iterable<?> findList() {
                return Collections.singleton(finder.count());
            }
        };
        return new QueryExectuor() {
            @Override
            protected Iterable<?> findList() {
                return finder.findAll();
            }
        };
    }

    abstract static class QueryExectuor {
        protected abstract Iterable<?> findList();

    }

    private Class<NodeBacked> getEntityClass(final String shortName) {
        try {
            final String className = getFQN(shortName);
            return (Class<NodeBacked>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Error resolving class "+shortName,e);
        }
    }

    private String getFQN(final String shortName) throws ClassNotFoundException {
        for (final String className : info.getManagedClassNames()) {
            if (className.endsWith(shortName)) return className;
        }
        throw new ClassNotFoundException("No mapped class found for "+shortName);
    }

    @Override
    public List getResultList() {
        final List<Object> result = new ArrayList<Object>();
        int count=0;
        for (final Object nodeBacked : queryExectuor.findList()) {
            if (maxResult>=0 && count==startPosition+maxResult) break;
            if (count>=startPosition) {
                result.add(nodeBacked);
            }
            count++;
        }
        return result;
    }

    @Override
    public Object getSingleResult() {
        final Iterator<?> found = queryExectuor.findList().iterator();
        return found.hasNext() ? found.next() : null; // todo errors when none or too many ?
    }

    @Override
    public int executeUpdate() {
        return 0;
    }

    @Override
    public Query setMaxResults(final int maxResult) {
        this.maxResult = maxResult;
        return this;
    }

    @Override
    public Query setFirstResult(final int startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    @Override
    public Query setHint(final String hintName, final Object value) {
        return this;
    }

    @Override
    public Query setParameter(final String name, final Object value) {
        return this;
    }

    @Override
    public Query setParameter(final String name, final Date value, final TemporalType temporalType) {
        return this;
    }

    @Override
    public Query setParameter(final String name, final Calendar value, final TemporalType temporalType) {
        return this;
    }

    @Override
    public Query setParameter(final int position, final Object value) {
        return this;
    }

    @Override
    public Query setParameter(final int position, final Date value, final TemporalType temporalType) {
        return this;
    }

    @Override
    public Query setParameter(final int position, final Calendar value, final TemporalType temporalType) {
        return this;
    }

    @Override
    public Query setFlushMode(final FlushModeType flushMode) {
        return this;
    }
}
