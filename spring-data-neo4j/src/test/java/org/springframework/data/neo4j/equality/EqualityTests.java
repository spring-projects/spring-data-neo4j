/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.equality;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@NodeEntity
class ImproperEntity {
    @GraphId
    Long id;
}

@NodeEntity
class ProperEntity {
    @GraphId
    Long id;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (hashCode() != other.hashCode()) return false;

        if (id == null) return false;

        if (! (other instanceof ProperEntity)) return false;

        return id.equals(((ProperEntity) other).id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[%d, #%d]", getClass().getSimpleName(), id, hashCode());
    }
}

@NodeEntity
class HashCodeCachingEntity {
    @GraphId
    Long id;

    transient private Integer hash;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (hashCode() != other.hashCode()) return false;

        if (id == null) return false;

        if (! (other instanceof HashCodeCachingEntity)) return false;

        return id.equals(((HashCodeCachingEntity) other).id);
    }

    @Override
    public int hashCode() {
        if (hash == null) hash = id == null ? System.identityHashCode(this) : id.hashCode();

        return hash.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[%d, #%d]", getClass().getSimpleName(), id, hashCode());
    }
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class EqualityTests {
    @Autowired
    private Neo4jTemplate template;

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        TestConfig() throws ClassNotFoundException {
            setBasePackage("org.springframework.data.neo4j.equality");
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    @Test
    public void entitiesThatDoNotImplementEqualsAndHashCodeShouldNotBeConsistentAfterSaving() throws Exception {
        Set<ImproperEntity> entities = new HashSet<ImproperEntity>();
        ImproperEntity entity = new ImproperEntity();
        entities.add(entity);
        ImproperEntity savedEntity = template.save(entity);
        entities.add(savedEntity);
        ImproperEntity foundEntity = template.findOne(entity.id, ImproperEntity.class);
        entities.add(foundEntity);

        assertThat(entity, is(not(savedEntity)));
        assertThat(entity, is(not(foundEntity)));
        assertThat(savedEntity, is(not(foundEntity)));

        assertThat(entities.contains(entity), is(true));
        assertThat(entities.contains(savedEntity), is(true));
        assertThat(entities.contains(foundEntity), is(true));

        assertThat(entities.remove(entity), is(true));
        assertThat(entities.remove(savedEntity), is(true));
        assertThat(entities.remove(foundEntity), is(true));
    }

    @Test
    public void entitiesThatImplementEqualsAndHashCodeShouldBeConsistentAfterSaving() throws Exception {
        Set<ProperEntity> entities = new HashSet<ProperEntity>();
        ProperEntity entity = new ProperEntity();
        entities.add(entity);
        ProperEntity savedEntity = template.save(entity);
        entities.add(savedEntity);
        ProperEntity foundEntity = template.findOne(entity.id, ProperEntity.class);
        entities.add(foundEntity);

        assertThat(entity, is(savedEntity));
        assertThat(entity, is(foundEntity));
        assertThat(savedEntity, is(foundEntity));

        assertThat(entities.size(), is(2));

        assertThat(entities.contains(entity), is(true));
        assertThat(entities.contains(savedEntity), is(true));
        assertThat(entities.contains(foundEntity), is(true));

        assertThat(entities.remove(entity), is(true));
        assertThat(entities.remove(savedEntity), is(false));
        assertThat(entities.remove(foundEntity), is(false));

        assertThat(entities.iterator().next(), is(entity));
    }

    @Test
    public void entitiesThatCacheHashCodeShouldBeConsistentAfterSaving() throws Exception {
        Set<HashCodeCachingEntity> entities = new HashSet<HashCodeCachingEntity>();
        HashCodeCachingEntity entity = new HashCodeCachingEntity();
        entities.add(entity);
        HashCodeCachingEntity savedEntity = template.save(entity);
        entities.add(savedEntity);
        HashCodeCachingEntity foundEntity = template.findOne(entity.id, HashCodeCachingEntity.class);
        entities.add(foundEntity);

        assertThat(entity, is(not(savedEntity)));
        assertThat(entity, is(not(foundEntity)));
        assertThat(savedEntity, is(foundEntity));

        assertThat(entities.size(), is(2));

        assertThat(entities.contains(entity), is(true));
        assertThat(entities.contains(savedEntity), is(true));
        assertThat(entities.contains(foundEntity), is(true));

        assertThat(entities.remove(entity), is(true));
        assertThat(entities.remove(savedEntity), is(true));
        assertThat(entities.remove(foundEntity), is(false));

        assertThat(entities.size(), is(0));
    }
}
