package org.springframework.data.neo4j.repository.query;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

import static org.springframework.data.repository.util.ClassUtils.hasParameterOfType;

public class ReactiveGraphQueryMethod extends GraphQueryMethod {
	private static final ClassTypeInformation<Page> PAGE_TYPE = ClassTypeInformation.from(Page.class);
	private static final ClassTypeInformation<Slice> SLICE_TYPE = ClassTypeInformation.from(Slice.class);

	private final Method method;

	public ReactiveGraphQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);

		if (hasParameterOfType(method, Pageable.class)) {

			TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);

			boolean multiWrapper = ReactiveWrappers.isMultiValueType(returnType.getType());
			boolean singleWrapperWithWrappedPageableResult = ReactiveWrappers.isSingleValueType(returnType.getType())
					&& (PAGE_TYPE.isAssignableFrom(returnType.getRequiredComponentType())
							|| SLICE_TYPE.isAssignableFrom(returnType.getRequiredComponentType()));

			if (singleWrapperWithWrappedPageableResult) {
				throw new InvalidDataAccessApiUsageException(
						String.format("'%s.%s' must not use sliced or paged execution. Please use Flux.buffer(size, skip).",
								ClassUtils.getShortName(method.getDeclaringClass()), method.getName()));
			}

			if (!multiWrapper && !singleWrapperWithWrappedPageableResult) {
				throw new IllegalStateException(String.format(
						"Method has to use a either multi-item reactive wrapper return type or a wrapped Page/Slice type. Offending method: %s",
						method.toString()));
			}

			if (hasParameterOfType(method, Sort.class)) {
				throw new IllegalStateException(String.format("Method must not have Pageable *and* Sort parameter. "
						+ "Use sorting capabilities on Pageble instead! Offending method: %s", method.toString()));
			}
		}

		this.method = method;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#isCollectionQuery()
	 */
	@Override
	public boolean isCollectionQuery() {
		return !(isPageQuery() || isSliceQuery()) && ReactiveWrappers.isMultiValueType(method.getReturnType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#isModifyingQuery()
	 */
	@Override
	public boolean isModifyingQuery() {
		return super.isModifyingQuery();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#isQueryForEntity()
	 */
	@Override
	public boolean isQueryForEntity() {
		return super.isQueryForEntity();
	}

	/*
	 * All reactive query methods are streaming queries.
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#isStreamQuery()
	 */
	@Override
	public boolean isStreamQuery() {
		return true;
	}

	/**
	 * Check if the given {@link org.springframework.data.repository.query.QueryMethod} receives a reactive parameter
	 * wrapper as one of its parameters.
	 *
	 * @return
	 */
	public boolean hasReactiveWrapperParameter() {

		for (GraphParameters.GraphParameter graphParameter : getParameters()) {
			if (ReactiveWrapperConverters.supports(graphParameter.getType())) {
				return true;
			}
		}
		return false;
	}

	TypeInformation<?> getReturnType() {
		return ClassTypeInformation.fromReturnTypeOf(method);
	}
}
