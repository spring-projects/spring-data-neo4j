package school.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Service;
import school.domain.Teacher;
import school.repository.TeacherRepository;

@Service("teacherService")
public class TeacherServiceImpl extends GenericService<Teacher> implements TeacherService {

    @Autowired()
    private TeacherRepository teacherRepository;


    @Override
    public GraphRepository<Teacher> getRepository() {
        return teacherRepository;
    }
}
