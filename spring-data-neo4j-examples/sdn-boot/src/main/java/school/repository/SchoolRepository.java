package school.repository;

import school.domain.School;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolRepository extends GraphRepository<School> {

}
