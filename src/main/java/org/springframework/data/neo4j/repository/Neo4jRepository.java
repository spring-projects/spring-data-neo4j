/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.repository;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 * Neo4j specific {@link org.springframework.data.repository.Repository} interface.
 *
 * @param <T> type of the domain class to map
 * @param <ID> identifier type in the domain class
 * @author Michael J. Simons
 * @author Ján Šúr
 * @since 6.0
 */
@NoRepositoryBean
public interface Neo4jRepository<T, ID>
		extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T>, CrudRepository<T, ID> {

	@Override
	<S extends T> List<S> saveAll(Iterable<S> entities);

	@Override
	List<T> findAll();

	@Override
	List<T> findAllById(Iterable<ID> iterable);

	@Override
	List<T> findAll(Sort sort);

	@Override
	<S extends T> List<S> findAll(Example<S> example);

	@Override
	<S extends T> List<S> findAll(Example<S> example, Sort sort);

}
