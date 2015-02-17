package school.service;

import org.springframework.data.neo4j.repository.GraphRepository;
import school.domain.Student;
import school.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service("studentService")
public class StudentServiceImpl extends GenericService<Student> implements StudentService {

    @Autowired
    private StudentRepository studentRepository;


    @Override
    public GraphRepository<Student> getRepository() {
        return studentRepository;
    }
}
