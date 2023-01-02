/*
 * Copyright 2011-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.PropertyHandlerSupport;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.projection.MethodInterceptorFactory;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Basically a lenient property accessing method interceptor, first trying the entity property (or attribute), than
 * a potentially renamed attribute via {@link Property}.
 *
 * @author Michael J. Simons
 */
final class EntityAndGraphPropertyAccessingMethodInterceptor implements MethodInterceptor {

	static MethodInterceptorFactory createMethodInterceptorFactory(Neo4jMappingContext mappingContext) {
		return new MethodInterceptorFactory() {
			@Override
			public MethodInterceptor createMethodInterceptor(Object source, Class<?> targetType) {
				return new EntityAndGraphPropertyAccessingMethodInterceptor(source, mappingContext);
			}

			@Override public boolean supports(Object source, Class<?> targetType) {
				return true;
			}
		};
	}

	private final BeanWrapper target;

	private EntityAndGraphPropertyAccessingMethodInterceptor(Object target, Neo4jMappingContext ctx) {

		Assert.notNull(target, "Proxy target must not be null");
		this.target = new GraphPropertyAndDirectFieldAccessFallbackBeanWrapper(target, ctx);
	}

	@Nullable
	@Override
	public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		if (ReflectionUtils.isObjectMethod(method)) {
			return invocation.proceed();
		}

		PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method);

		if (descriptor == null) {
			throw new IllegalStateException("Invoked method is not a property accessor");
		}

		if (!isSetterMethod(method, descriptor)) {
			return target.getPropertyValue(descriptor.getName());
		}

		if (invocation.getArguments().length != 1) {
			throw new IllegalStateException("Invoked setter method requires exactly one argument");
		}

		target.setPropertyValue(descriptor.getName(), invocation.getArguments()[0]);
		return null;
	}

	private static boolean isSetterMethod(Method method, PropertyDescriptor descriptor) {
		return method.equals(descriptor.getWriteMethod());
	}

	/**
	 * this version of the {@link DirectFieldAccessFallbackBeanWrapper} checks if there's an attribute on the entity
	 * annotated with {@link Property} mapping it to a different graph property when it fails to access the original
	 * attribute If so, that property is accessed. If not, the original exception is rethrown.
	 * This helps in projections such as described here
	 * https://stackoverflow.com/questions/68938823/sdn6-projection-interfaces-with-property-mapping
	 * that could have been used as workaround prior to fixing 2371.
	 */
	static class GraphPropertyAndDirectFieldAccessFallbackBeanWrapper extends DirectFieldAccessFallbackBeanWrapper {

		private final Neo4jMappingContext ctx;

		GraphPropertyAndDirectFieldAccessFallbackBeanWrapper(Object target, Neo4jMappingContext ctx) {
			super(target);
			this.ctx = ctx;
		}

		@Override
		public Object getPropertyValue(String propertyName) {
			try {
				return super.getPropertyValue(propertyName);
			} catch (NotReadablePropertyException e) {
				Neo4jPersistentEntity<?> entity = ctx.getPersistentEntity(super.getRootClass());

				AtomicReference<String> value = new AtomicReference<>();
				if (entity != null) {
					PropertyHandlerSupport.of(entity).doWithProperties(
							p -> {
								if (p.findAnnotation(Property.class) != null && p.getPropertyName()
										.equals(propertyName)) {
									value.compareAndSet(null, p.getFieldName());
								}
							});
					if (value.get() != null) {
						return super.getPropertyValue(value.get());
					}
				}
				throw e;
			}
		}
	}
}
