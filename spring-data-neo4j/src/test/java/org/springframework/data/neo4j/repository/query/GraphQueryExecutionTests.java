/*
 * Copyright 2011-2020 the original author or authors.
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
	public void pagedExecutionShouldNotGenerateCountQueryIfQueryReportedNoResults() {

		when(sessionMock.query(eq(User.class), anyString(), anyMap())).thenReturn(Collections.<User> emptyList());

		GraphQueryMethod queryMethod = new GraphQueryMethod(method, new DefaultRepositoryMetadata(UserRepository.class),
				factory);
		GraphParameterAccessor accessor = new GraphParametersParameterAccessor(queryMethod,
				new Object[] { "", PageRequest.of(0, 1) });
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
