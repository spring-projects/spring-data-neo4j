package org.springframework.data.neo4j.repositories.repo.reactive;

import org.neo4j.ogm.model.Result;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.domain.sample.SampleEntity;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;

public interface ReactiveSampleEntityRepository extends ReactiveNeo4jRepository<SampleEntity, Long> {

	@Query("MATCH (n:SampleEntity) RETURN n")
	Flux<SampleEntity> getAllByQuery();

	Mono<SampleEntity> findByFirst(String first);

	Mono<Long> removeByFirst(String first);

	Flux<SampleEntity> findByQueryWithoutParameter();

	Mono<SampleEntity> findByQueryWithParameter(@Param("name") String name);

	@Query("MATCH (n:SampleEntity) SET n.updated=timestamp()")
	Mono<Result> touchAllSampleEntitiesWithStatistics();

	Mono<Long> countByFirst(String first);

	Flux<SampleEntity> findByFirstIn(Observable<String> first);
}
