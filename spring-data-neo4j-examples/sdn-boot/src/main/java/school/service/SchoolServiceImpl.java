package school.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Service;
import school.domain.School;
import school.repository.SchoolRepository;

@Service("schoolService")
public class SchoolServiceImpl extends GenericService<School> implements SchoolService {

    @Autowired
    private SchoolRepository repository;

    @Override
    public GraphRepository<School> getRepository() {
        return repository;
    }

}
