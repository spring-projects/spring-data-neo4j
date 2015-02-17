package school.repository;

import school.domain.Teacher;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends GraphRepository<Teacher> {

}
