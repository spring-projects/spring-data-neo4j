package org.springframework.datastore.graph.neo4j.jpa;

import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.finder.Finder;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.datastore.graph.neo4j.support.Tuple2;

import javax.persistence.*;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.datastore.graph.neo4j.jpa.Neo4jQueryParameter.param;
import static org.springframework.datastore.graph.neo4j.support.Tuple2._;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

/**
* @author Michael Hunger
* @since 29.08.2010
*/
public class Neo4JQuery<T extends NodeBacked> implements TypedQuery<T> {
    protected final Finder<T> finder;
    protected final Class<T> entityClass;
    protected final String qlString;
    private final PersistenceUnitInfo info;
    private final Pattern fromPattern = Pattern.compile("^.*\\bfrom\\s+([A-Z][A-Za-z0-9]+)\\b.*");
    private int startPosition=0;
    private int maxResult=-1;
    private QueryExectuor<?> queryExectuor;
    private Map<Parameter<?>, Tuple2<?,TemporalType>> parameters=new HashMap<Parameter<?>, Tuple2<?,TemporalType>>();

    public Neo4JQuery(final String qlString, final FinderFactory finderFactory, final PersistenceUnitInfo info, final Class<T> entityClass) {
        this.qlString = qlString;
        this.info = info;
        final Matcher matcher = fromPattern.matcher(qlString);
        if (matcher.matches()) {
            final String shortName = matcher.group(1);
            if (entityClass!=null) this.entityClass= entityClass;
            else this.entityClass=getEntityClass(shortName);
            finder = finderFactory.getFinderForClass(this.entityClass);
            queryExectuor = createExecutor(qlString);
        } else {
            throw new IllegalAccessError("Unable to parse query "+qlString);
        }
    }

    private QueryExectuor<?> createExecutor(final String qlString) {
        if (qlString.contains(" count(")) return new QueryExectuor<Long>() {
            @Override
            protected Long findObject() {
                return finder.count();
            }
        };
        return new QueryExectuor<T>() {
            @Override
            protected Iterable<T> findList() {
                return finder.findAll();
            }
        };
    }

    abstract static class QueryExectuor<T> {
        protected Iterable<T> findList() { return Collections.singleton(findObject()); }
        protected T findObject() { return null; }

    }

    private Class<T> getEntityClass(final String shortName) {
        try {
            final String className = getFQN(shortName);
            return (Class<T>) Class.forName(className);
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
    public List<T> getResultList() {
        final List<T> result = new ArrayList<T>();
        int count=0;
        for (final T nodeBacked : (Iterable<T>)queryExectuor.findList()) {
            if (maxResult>=0 && count==startPosition+maxResult) break;
            if (count>=startPosition) {
                result.add(nodeBacked);
            }
            count++;
        }
        return result;
    }

    @Override
    public T getSingleResult() {
        final Iterator<?> found = queryExectuor.findList().iterator();
        return found.hasNext() ? (T) found.next() : null; // todo errors when none or too many ?
    }

    @Override
    public int executeUpdate() {
        return 0;
    }

    @Override
    public TypedQuery<T> setMaxResults(final int maxResult) {
        this.maxResult = maxResult;
        return this;
    }

    @Override
    public int getMaxResults() {
        return maxResult;
    }

    @Override
    public TypedQuery<T> setFirstResult(final int startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    @Override
    public int getFirstResult() {
        return startPosition;
    }

    @Override
    public TypedQuery<T> setHint(final String hintName, final Object value) {
        return this;
    }

    @Override
    public Map<String, Object> getHints() {
        return Collections.emptyMap();
    }

    @Override
    public <P> TypedQuery<T> setParameter(final Parameter<P> parameter, final P value) {
        this.parameters.put(parameter, value(value));
        return this;
    }

    private static <P> Tuple2<P, TemporalType> value(final P value) {
        return _(value,(TemporalType)null);
    }

    @Override
    public TypedQuery<T> setParameter(final Parameter<Calendar> parameter, final Calendar calendar, final TemporalType temporalType) {
        this.parameters.put(parameter,_(calendar,temporalType));
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(final Parameter<Date> parameter, final Date date, final TemporalType temporalType) {
        this.parameters.put(parameter,_(date,temporalType));
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(final String name, final Object value) {
        this.parameters.put(param(name),value(value));
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(final String name, final Date value, final TemporalType temporalType) {
        this.parameters.put(param(name),_(value,temporalType));
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(final String name, final Calendar value, final TemporalType temporalType) {
        this.parameters.put(param(name),_(value,temporalType));
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(final int position, final Object value) {
        this.parameters.put(param(position),value(value));
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(final int position, final Date value, final TemporalType temporalType) {
        this.parameters.put(param(position),value(value));
        return this;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return parameters.keySet();
    }

    @Override
    public Parameter<?> getParameter(final String name) {
        for (final Parameter<?> parameter : parameters.keySet()) {
            if (nullSafeEquals(parameter.getName(),name))  return parameter;
        }
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(final String name, final Class<T> type) {
        for (final Parameter<?> parameter : parameters.keySet()) {
            if (nullSafeEquals(parameter.getName(),name) && nullSafeEquals(type,parameter.getParameterType()))  return (Parameter<T>) parameter;
        }
        return null;
    }

    @Override
    public Parameter<?> getParameter(final int index) {
        for (final Parameter<?> parameter : parameters.keySet()) {
            if (nullSafeEquals(parameter.getPosition(),index))  return parameter;
        }
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(final int index, final Class<T> type) {
        for (final Parameter<?> parameter : parameters.keySet()) {
            if (nullSafeEquals(parameter.getPosition(),index) && nullSafeEquals(type,parameter.getParameterType()))  return (Parameter<T>) parameter;
        }
        return null;
    }

    @Override
    public boolean isBound(final Parameter<?> parameter) {
        return parameters.containsKey(parameter);
    }

    @Override
    public <T> T getParameterValue(final Parameter<T> parameter) {
        return (T) parameters.get(parameter)._1;
    }

    @Override
    public Object getParameterValue(final String name) {
        return getParameterValue(getParameter(name));
    }

    @Override
    public Object getParameterValue(final int index) {
        return getParameterValue(getParameter(index));
    }

    @Override
    public TypedQuery<T> setParameter(final int position, final Calendar value, final TemporalType temporalType) {
        parameters.put(param(position),_(value,temporalType));
        return this;
    }

    @Override
    public TypedQuery<T> setFlushMode(final FlushModeType flushMode) {
        return this;
    }

    @Override
    public FlushModeType getFlushMode() {
        return null;
    }

    @Override
    public TypedQuery<T> setLockMode(final LockModeType lockModeType) {
        return null;
    }

    @Override
    public LockModeType getLockMode() {
        return null;
    }

    @Override
    public <T> T unwrap(final Class<T> tClass) {
        return null;
    }
}
