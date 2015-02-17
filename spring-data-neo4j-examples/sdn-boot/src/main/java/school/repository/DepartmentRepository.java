package school.repository;

import school.domain.Department;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends GraphRepository<Department> {

}
