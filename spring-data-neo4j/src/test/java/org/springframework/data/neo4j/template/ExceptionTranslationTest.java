package org.springframework.data.neo4j.template;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@ContextConfiguration(classes = DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class ExceptionTranslationTest {

    @Autowired
    private Neo4jOperations neo4jTemplate;


    @Before
    public void setUp()  {
        Assert.notNull(neo4jTemplate, "neo4jTemplate not properly wired in");
    }

    @Test(expected= DataAccessException.class)
    @Ignore("this isn't working as the docs say it should. We must be doing something wrong")
    public void testTemplateExceptionsAreIntercepted() {
        neo4jTemplate.loadAll(Void.class);

    }

}
