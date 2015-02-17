package org.springframework.data.neo4j.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GraphRepository<T> extends CrudRepository<T, Long> {

    <S extends T> S save(S s, int depth);

    <S extends T> Iterable<S> save(Iterable<S> entities, int depth);

    T findOne(Long id, int depth);

    Iterable<T> findAll(int depth);

    Iterable<T> findAll(Iterable<Long> ids, int depth);

    Iterable<T> findByProperty(String propertyName, Object propertyValue);

    Iterable<T> findByProperty(String propertyName, Object propertyValue, int depth);
}
