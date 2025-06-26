package org.springframework.data.neo4j.integration.imperative;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.mapping.callback.BeforeBindCallback;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Book;
import org.springframework.data.neo4j.integration.shared.common.ImmutableAuditableThing;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
public class AggregateLimitingIT {

    protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

    @BeforeEach
    void setup(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
        try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
            session.run("MATCH (n) detach delete n").consume();
            session
                .run("CREATE (se:StartEntity)-[:CONNECTED]->(ie:IntermediateEntity)-[:CONNECTED]->(dae:DifferentAggregateEntity{name:'some_name'})")
                .consume();
            bookmarkCapture.seedWith(session.lastBookmarks());
        }
    }

    @AfterEach
    void tearDown(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
        try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
            session.run("MATCH (n) detach delete n").consume();
            bookmarkCapture.seedWith(session.lastBookmarks());
        }
    }

    @Test
    void shouldOnlyReportIdForDifferentAggregateEntity(@Autowired AggregateRepository repository) {
        var startEntity = repository.findAllBy().get(0);

        assertThat(startEntity).isNotNull();
        assertThat(startEntity.getId()).isNotNull();
        assertThat(startEntity.getIntermediateEntity()).isNotNull();
        assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
        assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
        assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
        assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
    }


    @Node
    public static class StartEntity {
        @Id
        @GeneratedValue
        public String id;

        @Relationship("CONNECTED")
        IntermediateEntity intermediateEntity;

        public IntermediateEntity getIntermediateEntity() {
            return intermediateEntity;
        }

        public String getId() {
            return id;
        }
    }

    @Node
    public static class IntermediateEntity {
        @Id
        @GeneratedValue
        public String id;

        @Relationship("CONNECTED")
        DifferentAggregateEntity differentAggregateEntity;

        public DifferentAggregateEntity getDifferentAggregateEntity() {
            return differentAggregateEntity;
        }

        public String getId() {
            return id;
        }
    }

    @Node(stopCascadingFrom = StartEntity.class)
    public static class DifferentAggregateEntity {
        @Id
        @GeneratedValue
        public String id;

        public final String name;

        public DifferentAggregateEntity(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    interface StartEntityProjection {
        IntermediateEntityProjection getIntermediateEntity();
    }

    interface IntermediateEntityProjection {
        DifferentAggregateEntityProjection getDifferentAggregateEntity();
    }

    interface DifferentAggregateEntityProjection {
        String getId();
    }

    interface AggregateRepository extends Neo4jRepository<StartEntity, String> {
        List<StartEntity> findAllBy();
    }

    @Configuration
    @EnableTransactionManagement
    @EnableNeo4jRepositories(considerNestedRepositories = true)
    static class Config extends Neo4jImperativeTestConfiguration {

        @Bean
        @Override
        public Driver driver() {
            return neo4jConnectionSupport.getDriver();
        }

        @Override
        protected Collection<String> getMappingBasePackages() {
            return Collections.singleton(AggregateLimitingIT.class.getPackage().getName());
        }

        @Bean
        BookmarkCapture bookmarkCapture() {
            return new BookmarkCapture();
        }

        @Override
        public PlatformTransactionManager transactionManager(Driver driver,
                                                             DatabaseSelectionProvider databaseNameProvider) {

            BookmarkCapture bookmarkCapture = bookmarkCapture();
            return new Neo4jTransactionManager(driver, databaseNameProvider,
                    Neo4jBookmarkManager.create(bookmarkCapture));
        }

        @Override
        public boolean isCypher5Compatible() {
            return neo4jConnectionSupport.isCypher5SyntaxCompatible();
        }

    }
}
