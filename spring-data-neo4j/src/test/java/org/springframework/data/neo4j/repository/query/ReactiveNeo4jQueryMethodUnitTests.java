/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.query;

import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.typeCompatibleWith;
import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link org.springframework.data.neo4j.repository.query.ReactiveGraphRepositoryQuery}.
 *
 * @author lilit gabrielyan
 */
public class ReactiveNeo4jQueryMethodUnitTests {

	@Test(expected = IllegalStateException.class)
	public void rejectsMonoPageableResult() throws Exception {
		queryMethod(UserRepository.class, "findMonoByLastname", String.class, Pageable.class);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void throwsExceptionOnWrappedPage() throws Exception {
		queryMethod(UserRepository.class, "findMonoPageByLastname", String.class, Pageable.class);
	}

	@Test(expected = IllegalStateException.class)
	public void throwsExceptionOnWrappedSortedPage() throws Exception {
		queryMethod(UserRepository.class, "findMonoPageSortedByLastname", String.class, Pageable.class, Sort.class);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void throwsExceptionOnWrappedSlice() throws Exception {
		queryMethod(UserRepository.class, "findMonoSliceByLastname", String.class, Pageable.class);
	}

	private ReactiveGraphQueryMethod queryMethod(Class<?> repository, String name, Class<?>... parameters)
			throws Exception {

		Method method = repository.getMethod(name, parameters);
		ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		return new ReactiveGraphQueryMethod(method, new DefaultRepositoryMetadata(repository), factory);
	}

	interface UserRepository extends ReactiveNeo4jRepository<User, Long> {

		Mono<User> findMonoByLastname(String lastname, Pageable pageRequest);

		Mono<Page<User>> findMonoPageByLastname(String lastname, Pageable pageRequest);

		Mono<Page<User>> findMonoPageSortedByLastname(String lastname, Pageable pageRequest, Sort sort);

		Mono<Slice<User>> findMonoSliceByLastname(String lastname, Pageable pageRequest);

		Flux<User> findByLocationNear(Point point, Distance distance);

		Flux<User> findByLocationNear(Point point, Distance distance, Pageable pageable);

	}

}
