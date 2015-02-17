package school.service;

import org.springframework.data.neo4j.repository.GraphRepository;
import school.domain.Entity;

public abstract class GenericService<T> implements Service<T> {

    private static final int DEPTH_LIST = 0;
    private static final int DEPTH_ENTITY = 1;

    @Override
    public Iterable<T> findAll() {
        return getRepository().findAll(DEPTH_LIST);
    }

    @Override
    public T find(Long id) {
        return getRepository().findOne(id, DEPTH_ENTITY);
    }

    @Override
    public void delete(Long id) {
        getRepository().delete(id);
    }

    @Override
    public T createOrUpdate(T entity) {
        getRepository().save(entity, DEPTH_ENTITY);
        return find(((Entity) entity).getId());
    }

    public abstract GraphRepository<T> getRepository();
}
