package org.springframework.data.neo4j.template;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@ContextConfiguration(classes = DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@DirtiesContext
public class ExceptionTranslationTest extends MultiDriverTestClass {

    @Autowired
    private Neo4jOperations neo4jTemplate;


    @Before
    public void setUp()  {
        Assert.notNull(neo4jTemplate, "neo4jTemplate not properly wired in");
    }

    @Test(expected= InvalidDataAccessApiUsageException.class)
    public void testTemplateExceptionsAreIntercepted() {
        neo4jTemplate.loadAll(Rating.class, 0);

    }

}
