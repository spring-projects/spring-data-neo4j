package org.springframework.data.neo4j.repository.query;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ResultProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lilit gabrielyan
 */
public class ReactiveGraphRepositoryQuery extends AbstractReactiveGraphRepositoryQuery {

	private static final Logger LOG = LoggerFactory.getLogger(ReactiveGraphRepositoryQuery.class);
	private final ReactiveGraphQueryMethod graphQueryMethod;
	private final Session session;

	protected ReactiveGraphRepositoryQuery(ReactiveGraphQueryMethod graphQueryMethod, Session session) {
		super(graphQueryMethod, session);
		this.graphQueryMethod = graphQueryMethod;
		this.session = session;
	}

	@Override
	public Object doExecute(Query query, Object[] parameters) {

		return doExecute(query, new ReactiveGraphParameterAccessor(graphQueryMethod, parameters));
	}

	private Object doExecute(Query query, ReactiveGraphParameterAccessor accessor) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query for method {}", graphQueryMethod.getName());
		}

		Class<?> returnType = graphQueryMethod.getMethod().getReturnType();

		ResultProcessor processor = graphQueryMethod.getResultProcessor().withDynamicProjection(accessor);

		Object result = getExecution(accessor).execute(query, processor.getReturnedType().getReturnedType());

		return Result.class.equals(returnType) ? result
				: processor.processResult(result,
						new CustomResultConverter(getMetaData(), processor.getReturnedType().getReturnedType()));
	}

	// just an horrible trick to get the metadata from OGM
	private MetaData getMetaData() {
		return session.doInTransaction((requestHandler, transaction, metaData) -> metaData);
	}

	protected Query getQuery(Object[] parameters) {
		return new Query(getQueryString(), graphQueryMethod.getCountQueryString(), resolveParams(parameters));
	}

	private String getQueryString() {
		return getQueryMethod().getQuery();
	}

	protected Map<String, Object> resolveParams(Object[] parameters) {

		Map<String, Object> params = new HashMap<>();
		Parameters<?, ?> methodParameters = graphQueryMethod.getParameters();

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = methodParameters.getParameter(i);
			Object parameterValue = getParameterValue(parameters[i]);

			if (parameter.isExplicitlyNamed()) {
				parameter.getName().ifPresent(name -> params.put(name, parameterValue));
			} else {
				params.put("" + i, parameterValue);
			}
		}
		return params;
	}

	private Object getParameterValue(Object parameter) {

		// The parameter might be an entity, try to resolve its id
		Object parameterValue = session.resolveGraphIdFor(parameter);
		if (parameterValue == null) { // Either not an entity or not persisted
			parameterValue = parameter;
		}
		return parameterValue;
	}

	@Override
	protected boolean isCountQuery() {
		return false;
	}

	@Override
	protected boolean isDeleteQuery() {
		return false;
	}
}
