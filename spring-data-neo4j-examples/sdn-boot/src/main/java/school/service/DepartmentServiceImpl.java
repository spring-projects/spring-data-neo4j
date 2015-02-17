package school.service;

import org.springframework.data.neo4j.repository.GraphRepository;
import school.domain.Department;
import school.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("departmentService")
public class DepartmentServiceImpl extends GenericService<Department> implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public GraphRepository<Department> getRepository() {
        return departmentRepository;
    }
}
