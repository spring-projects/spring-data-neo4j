package org.springframework.data.neo4j.conversion;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.GraphProperty;
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
public class EnumConverterTests {

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        TestConfig() {
            setBasePackage("org.springframework.data.neo4j.conversion");
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
    static enum Vowel {
        A, E, I, O, U
    }
    static enum Consonant {
        B,C,D,F,G,H
    }

    @NodeEntity
    static class Word {
        @GraphId
        Long id;
        @Indexed(unique = true, indexType = IndexType.LABEL)
        Letter letter;
        @GraphProperty(propertyType = String.class)
        Vowel vowel;
        @GraphProperty(propertyType = Integer.class)
        Consonant consonant;

        Word() { }

        Word(Letter letter, Vowel vowel, Consonant consonant) {
            this.letter = letter;
            this.vowel = vowel;
            this.consonant = consonant;
        }
    }

    @Test
    @Transactional
    public void testConvertEnumsAccordingToAnnotation() throws Exception {
        Word word = template.save(new Word(Letter.C, Vowel.A, Consonant.B));
        assertEquals(Letter.C, word.letter);
        assertEquals(Vowel.A, word.vowel);
        assertEquals(Consonant.B, word.consonant);
        PropertyContainer node = template.getPersistentState(word);
        assertEquals(Letter.C.name(),node.getProperty("letter"));
        assertEquals(Vowel.A.name(),node.getProperty("vowel"));
        assertEquals(Consonant.B.ordinal(),node.getProperty("consonant"));
    }
}
