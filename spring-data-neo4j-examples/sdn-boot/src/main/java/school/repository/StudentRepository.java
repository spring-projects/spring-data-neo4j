package school.repository;

import school.domain.Student;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends GraphRepository<Student> {

}
