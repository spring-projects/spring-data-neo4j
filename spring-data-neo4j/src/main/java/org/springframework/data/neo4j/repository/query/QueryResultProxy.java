/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.query;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.session.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Method {@link InvocationHandler} used for proxy objects that implement arbitrary interfaces annotated with
 * <code>&#064;QueryResult</code>.
 *
 * @author Adam George
 */
class QueryResultProxy implements InvocationHandler {

	private static final Logger log = LoggerFactory.getLogger(QueryResultProxy.class);

	private static final Pattern beanGetterPattern = Pattern.compile("^(is|get)(\\w+)");

	private final Map<String, ?> data;

	QueryResultProxy(Map<String, ?> queryResults) {
		this.data = queryResults;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (isNotTraditionalGetter(method)) {
			log.warn("QueryResult interface method " + method.getName()
					+ " doesn't appear to be a getter and therefore may not return the correct result.");
		}

		if (method.isAnnotationPresent(Property.class)) {
			Property annotation = method.getAnnotation(Property.class);
			return Utils.coerceTypes(method.getReturnType(), data.get(annotation.name()));
		}

		Matcher matcher = beanGetterPattern.matcher(method.getName());
		if (matcher.matches()) {
			String propertyKey = matcher.group(2);
			propertyKey = propertyKey.substring(0, 1).toLowerCase().concat(propertyKey.substring(1));
			return Utils.coerceTypes(method.getReturnType(), data.get(propertyKey));
		}

		return Utils.coerceTypes(method.getReturnType(), data.get(method.getName()));
	}

	private boolean isNotTraditionalGetter(Method method) {
		return method.getParameterTypes().length != 0 || Void.class.equals(method.getReturnType())
				|| (!method.getName().startsWith("get") && !method.getName().startsWith("is"));
	}

}
