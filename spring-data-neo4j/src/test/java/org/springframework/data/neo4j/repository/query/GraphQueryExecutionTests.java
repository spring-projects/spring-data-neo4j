/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.neo4j.ogm.session.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.util.ReflectionUtils;

@RunWith(MockitoJUnitRunner.class)
public class GraphQueryExecutionTests {

	@Mock private Session sessionMock;

	private Method method = ReflectionUtils.findMethod(UserRepository.class, "findByFirstname", String.class,
			Pageable.class);
	private ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

	@Test
	@SuppressWarnings("unchecked")
	public void pagedExecutionShouldNotGenerateCountQueryIfQueryReportedNoResults() {

		when(sessionMock.query(eq(User.class), anyString(), anyMap())).thenReturn(Collections.<User> emptyList());

		GraphQueryMethod queryMethod = new GraphQueryMethod(method, new DefaultRepositoryMetadata(UserRepository.class),
				factory);
		GraphParameterAccessor accessor = new GraphParametersParameterAccessor(queryMethod,
				new Object[] { "", new PageRequest(0, 1) });
		GraphQueryExecution.PagedExecution execution = new GraphQueryExecution.PagedExecution(sessionMock, accessor);
		Query query = new Query("", "noop", new HashMap<>());
		execution.execute(query, User.class);

		verify(sessionMock).query(eq(User.class), anyString(), anyMap());
		verify(sessionMock, never()).queryForObject(eq(Integer.class), any(String.class), anyMap());
	}

	interface UserRepository extends Repository<User, Long> {

		Page<User> findByFirstname(String firstname, Pageable pageable);
	}

}
