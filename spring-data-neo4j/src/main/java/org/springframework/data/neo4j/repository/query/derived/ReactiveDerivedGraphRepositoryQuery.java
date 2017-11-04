package org.springframework.data.neo4j.repository.query.derived;

import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.repository.query.*;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.HashMap;
import java.util.Map;

/**
 * Specialisation of {@link RepositoryQuery} that handles mapping of derived finders.
 *
 * @author lilit gabrielyan
 */
public class ReactiveDerivedGraphRepositoryQuery extends AbstractReactiveGraphRepositoryQuery {

	private static final Logger LOG = LoggerFactory.getLogger(ReactiveDerivedGraphRepositoryQuery.class);
	private final DerivedQueryDefinition queryDefinition;
	private final ReactiveGraphQueryMethod graphQueryMethod;
	private final PartTree tree;

	public ReactiveDerivedGraphRepositoryQuery(ReactiveGraphQueryMethod graphQueryMethod, Session session) {
		super(graphQueryMethod, session);
		this.graphQueryMethod = graphQueryMethod;
		Class<?> domainType = graphQueryMethod.getEntityInformation().getJavaType();
		this.tree = new PartTree(graphQueryMethod.getName(), domainType);
		this.queryDefinition = new DerivedQueryCreator(tree, domainType).createQuery();

	}

	@Override
	protected Object doExecute(Query query, Object[] parameters) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query for method {}", graphQueryMethod.getName());
		}

		ReactiveGraphParameterAccessor accessor = new ReactiveGraphParameterAccessor(graphQueryMethod, parameters);
		Class<?> returnType = graphQueryMethod.getMethod().getReturnType();

		if (returnType.equals(Void.class)) {
			throw new RuntimeException("Derived Queries must have a return type");
		}

		ResultProcessor processor = graphQueryMethod.getResultProcessor().withDynamicProjection(accessor);
		Object results = getExecution(accessor).execute(query, processor.getReturnedType().getDomainType());

		return processor.processResult(results);
	}

	@Override
	protected Query getQuery(Object[] parameters) {
		return new Query(
				resolveParams(new ReactiveGraphParameterAccessor(graphQueryMethod, parameters), parameters.length));
	}

	@Override
	protected boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isDeleteQuery() {
		return tree.isDelete();
	}

	/**
	 * Sets values from parameters supplied by the finder on {@link org.neo4j.ogm.cypher.Filter} built by the
	 * {@link GraphQueryMethod}
	 *
	 * @param parameters parameter values supplied by the finder method
	 * @return List of Parameter with values set
	 */
	private Filters resolveParams(ReactiveGraphParameterAccessor parameters, int parametersCount) {
		Map<Integer, Object> params = new HashMap<>();

		for (int i = 0; i < parametersCount; i++) {
			if (graphQueryMethod.getQueryDepthParamIndex() == null
					|| (graphQueryMethod.getQueryDepthParamIndex() != null && graphQueryMethod.getQueryDepthParamIndex() != i)) {
				params.put(i, parameters.getBindableValue(i));
			}
		}

		return new Filters(queryDefinition.getFilters(params));
	}
}
