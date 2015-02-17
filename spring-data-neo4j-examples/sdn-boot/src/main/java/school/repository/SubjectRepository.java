package school.repository;

import school.domain.Subject;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends GraphRepository<Subject> {

}
