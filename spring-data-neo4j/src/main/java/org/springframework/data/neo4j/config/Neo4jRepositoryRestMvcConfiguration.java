package org.springframework.data.neo4j.config;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.neo4j.ogm.entityaccess.EntityFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.support.UnwrappingRepositoryInvokerFactory;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * Specialisation of Spring's {@link RepositoryRestMvcConfiguration} required to get SD-REST to work with this version of Spring
 * Data Neo4j.
 * <p>
 * This configuration ensures that {@link RepositoryInvoker}s return a cloned object from their {@code invokeFindOne()} methods
 * because the underlying Neo4j OGM will always return the same instance.  The Spring Data REST code relies on updating a
 * different instance from the one stored in the underlying OGM's mapping context, so by returning a cloned instance we prevent
 * problems with updates to entities.
 * </p>
 *
 * @author Adam George
 */
@Configuration
public class Neo4jRepositoryRestMvcConfiguration extends RepositoryRestMvcConfiguration {

    @Override
    @Bean
    public RepositoryInvokerFactory repositoryInvokerFactory() {
        return new UnwrappingRepositoryInvokerFactory(new DefaultRepositoryInvokerFactory(repositories(), defaultConversionService())) {
            @Override
            public RepositoryInvoker getInvokerFor(Class<?> domainType) {
                return new InstanceCloningRepositoryInvoker(super.getInvokerFor(domainType));
            }
        };
    }

    /**
     * Specialised {@link RepositoryInvoker} that returns cloned instances.
     */
    static final class InstanceCloningRepositoryInvoker implements RepositoryInvoker {

        private final RepositoryInvoker toWrap;
        private final EntityFactory entityFactory = new EntityFactory(null);

        private <T> T makeClone(final T original) {
            if (original == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            final T clone = (T) this.entityFactory.newObject(original.getClass());

            ReflectionUtils.doWithFields(original.getClass(), new FieldCallback() {
                @Override
                public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                    ReflectionUtils.makeAccessible(field);
                    Object value = field.get(original);
                    ReflectionUtils.setField(field, clone, value);
                }
            });

            return clone;
        }

        InstanceCloningRepositoryInvoker(RepositoryInvoker toWrap) {
            this.toWrap = toWrap;
        }

        @Override
        public <T> T invokeFindOne(Serializable id) {
            T object = toWrap.invokeFindOne(id);
            return makeClone(object);
        }

        @Override
        public boolean hasSaveMethod() {
            return toWrap.hasSaveMethod();
        }

        @Override
        public boolean hasFindOneMethod() {
            return toWrap.hasFindOneMethod();
        }

        @Override
        public boolean hasFindAllMethod() {
            return toWrap.hasFindAllMethod();
        }

        @Override
        public boolean hasDeleteMethod() {
            return toWrap.hasDeleteMethod();
        }

        @Override
        public <T> T invokeSave(T object) {
            return toWrap.invokeSave(object);
        }

        @Override
        public Object invokeQueryMethod(Method method, MultiValueMap<String, ? extends Object> parameters, Pageable pageable,
                Sort sort) {
            return toWrap.invokeQueryMethod(method, parameters, pageable, sort);
        }

        @SuppressWarnings("deprecation")
        @Override
        public Object invokeQueryMethod(Method method, Map<String, String[]> parameters, Pageable pageable, Sort sort) {
            return toWrap.invokeQueryMethod(method, parameters, pageable, sort);
        }

        @Override
        public Iterable<Object> invokeFindAll(Sort sort) {
            return toWrap.invokeFindAll(sort);
        }

        @Override
        public Iterable<Object> invokeFindAll(Pageable pageable) {
            return toWrap.invokeFindAll(pageable);
        }

        @Override
        public void invokeDelete(Serializable id) {
            toWrap.invokeDelete(id);
        }
    }

}
