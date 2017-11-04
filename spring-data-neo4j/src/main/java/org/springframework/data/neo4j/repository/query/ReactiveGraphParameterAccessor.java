package org.springframework.data.neo4j.repository.query;

import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link GraphParametersParameterAccessor} implementation using a {@link Parameters} instance to find special
 * parameters.
 *
 * @author lilit gabrielyan
 */
public class ReactiveGraphParameterAccessor extends GraphParametersParameterAccessor {

	private final Object[] values;
	private final List<MonoProcessor<?>> subscriptions;

	/**
	 * Creates a new {@link ReactiveGraphParameterAccessor}.
	 *
	 * @param method must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public ReactiveGraphParameterAccessor(GraphQueryMethod method, Object[] values) {

		super(method, values);
		this.values = values;
		this.subscriptions = new ArrayList<>(values.length);

		for (int i = 0; i < values.length; i++) {

			Object value = values[i];

			if (value == null || !ReactiveWrappers.supports(value.getClass())) {
				subscriptions.add(null);
				continue;
			}

			if (ReactiveWrappers.isSingleValueType(value.getClass())) {
				subscriptions.add(ReactiveWrapperConverters.toWrapper(value, Mono.class).toProcessor());
			} else {
				subscriptions.add(ReactiveWrapperConverters.toWrapper(value, Flux.class).collectList().toProcessor());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParametersParameterAccessor#getValue(int)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T> T getValue(int index) {

		if (subscriptions.get(index) != null) {
			return (T) subscriptions.get(index).block();
		}

		return super.getValue(index);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParametersParameterAccessor#getBindableValue(int)
	 */
	public Object getBindableValue(int index) {
		return getValue(getParameters().getBindableParameter(index).getIndex());
	}

}
