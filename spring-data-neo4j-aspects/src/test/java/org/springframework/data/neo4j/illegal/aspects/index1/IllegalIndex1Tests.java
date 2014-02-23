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

package org.springframework.data.neo4j.illegal.aspects.index1;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.aspects.support.EntityTestBase;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IllegalIndex1Tests extends EntityTestBase {

    @NodeEntity
    static class InvalidIndexed {

        @Indexed(indexType=IndexType.FULLTEXT)
        String fulltextNoIndexName;

        @Indexed(indexType=IndexType.FULLTEXT, indexName = "InvalidIndexed")
        String fullTextDefaultIndexName;

        public void setFulltextNoIndexName(String fulltextNoIndexName) {
            this.fulltextNoIndexName = fulltextNoIndexName;
        }

        public void setFullTextDefaultIndexName(String fullTextDefaultIndexName) {
            this.fullTextDefaultIndexName = fullTextDefaultIndexName;
        }
    }
    
    @NodeEntity
    static class InvalidSpatialIndexed1 {

        @Indexed(indexType=IndexType.POINT, indexName = "InvalidSpatialIndexed1")
        String wkt;

        public void setWkt(String wkt) {
            this.wkt = wkt;
        }
    }
    @NodeEntity
    static class InvalidSpatialIndexed2 {

        @Indexed(indexType=IndexType.POINT)
        String wkt;

        public void setWkt(String wkt) {
            this.wkt = wkt;
        }
    }
    @NodeEntity
    static class InvalidSpatialIndexed3 {


        @Indexed(indexType=IndexType.POINT, indexName = "pointLayer")
        String wkt;

        public void setWkt(String wkt) {
            this.wkt = wkt;
        }
    }

    @Test(expected = IllegalStateException.class)
    @Transactional
    public void indexAccessWithFullAndNoSpatialIndexNameShouldFail() throws Throwable {

        // Previously (prior to strict mode and base entity registration
        // requirements) this would only blow up when actually
        // attempting to do something illegal - now everything blows up on startup

        createAppCtxAndPropagateRootExceptionIfThrown(InvalidSpatialIndexed1.class);

        //InvalidSpatialIndexed1 invalidIndexed = persist(new InvalidSpatialIndexed1());
        //String latlon = "POINT (55 15)";
        //invalidIndexed.setWkt(latlon);
    }

    @Test(expected = IllegalStateException.class)
    @Transactional
    public void indexAccessWithDefaultSpatialIndexNameShouldFail() throws Throwable{

        // Previously (prior to strict mode and base entity registration
        // requirements) this would only blow up when actually
        // attempting to do something illegal - now everything blows up on startup

        createAppCtxAndPropagateRootExceptionIfThrown(InvalidSpatialIndexed2.class);

        //InvalidSpatialIndexed2 invalidIndexed = persist(new InvalidSpatialIndexed2());
        //String latlon = "POINT (55 15)";
        //invalidIndexed.setWkt( latlon);
    }


    @Test(expected = IllegalStateException.class)
    @Transactional
    public void indexAccessWithFullAndNoIndexNameShouldFail() throws Throwable{

        // Previously (prior to strict mode and base entity registration
        // requirements) this would only blow up when actually
        // attempting to do something illegal - now everything blows up on startup

        createAppCtxAndPropagateRootExceptionIfThrown(InvalidIndexed.class);

        //InvalidIndexed invalidIndexed = persist(new InvalidIndexed());
        //invalidIndexed.setFulltextNoIndexName(NAME_VALUE);
    }

    /**
     * As the first illegal entity detected will blow up the application context - we need a way
     * to ensure only the illegal entity under test it loaded to assert that we fail
     * for the correct reason and in an appropriate way. This method will create and application
     * context ensuring that only the illegal entity under test (passed in as an argument), is
     * detected by the context.  This is currently done by wrapping each Illegal Entity bootstrap
     * logic in a Spring profile against its same name
     *
     * @param entityUnderTest Class which should be detected by SDN for the purposes of testing
     * @throws Throwable
     */
    private void createAppCtxAndPropagateRootExceptionIfThrown(Class entityUnderTest) throws Throwable {
        try {
            ClassPathXmlApplicationContext appCtx = new ClassPathXmlApplicationContext();
            appCtx.setConfigLocation("org/springframework/data/neo4j/aspects/support/illegal-index1-tests-context.xml");
            appCtx.getEnvironment().setActiveProfiles( entityUnderTest.getSimpleName() );
            appCtx.refresh();
        } catch (BeanCreationException bce) {
            throw ExceptionUtils.getRootCause(bce);
        }
    }


}
