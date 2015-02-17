package school.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import school.domain.Course;

public interface CourseRepository extends GraphRepository<Course> {

}

