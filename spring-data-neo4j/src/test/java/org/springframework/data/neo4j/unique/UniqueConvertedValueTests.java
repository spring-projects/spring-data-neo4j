package org.springframework.data.neo4j.unique;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 30.06.14
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class UniqueConvertedValueTests {

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        TestConfig() {
            setBasePackage("org.springframework.data.neo4j.unique");
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    @Autowired
    Neo4jTemplate template;

    static enum Letter {
        A, B, C
    }

    @NodeEntity
    static class Word {
        @GraphId
        Long id;
        @Indexed(unique = true, indexType = IndexType.LABEL)
        Letter letter;
        String word;

        Word() { }

        Word(Letter letter, String word) {
            this.letter = letter;
            this.word = word;
        }
    }
    @NodeEntity
    static class Word2 {
        @GraphId
        Long id;
        @Indexed(unique = true, indexType = IndexType.SIMPLE)
        Letter letter;
        String word;

        Word2() { }

        Word2(Letter letter, String word) {
            this.letter = letter;
            this.word = word;
        }
    }

    @Test
    @Transactional
    public void testPersistUniqueEnumValue() throws Exception {
        Word word = template.save(new Word(Letter.A, "Afternoon"));
        assertEquals(Letter.A, word.letter);
        assertEquals("Afternoon", word.word);
    }
    @Test
    @Transactional
    public void testPersistUniqueEnumValueLegacyIndex() throws Exception {
        Word word = template.save(new Word(Letter.A, "Afternoon"));
        assertEquals(Letter.A, word.letter);
        assertEquals("Afternoon", word.word);
    }
}
