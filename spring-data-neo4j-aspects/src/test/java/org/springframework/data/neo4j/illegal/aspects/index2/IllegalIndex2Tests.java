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

package org.springframework.data.neo4j.illegal.aspects.index2;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IllegalIndex2Tests  {

    private static final String NAME_VALUE = "aName";
    private static final String NAME_VALUE2 = "aSecondName";

    @NodeEntity
    static class InvalidIndexed {

        @Indexed(indexType=IndexType.FULLTEXT)
        String fulltextNoIndexName;

        public void setFulltextNoIndexName(String fulltextNoIndexName) {
            this.fulltextNoIndexName = fulltextNoIndexName;
        }
    }

    @Test(expected = IllegalStateException.class)
    @Transactional
    public void indexAccessWithFullAndNoIndexNameShouldFail() throws Throwable {

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
            appCtx.setConfigLocation("org/springframework/data/neo4j/aspects/support/illegal-index2-tests-context.xml");
            appCtx.getEnvironment().setActiveProfiles( entityUnderTest.getSimpleName() );
            appCtx.refresh();
        } catch (BeanCreationException bce) {
            throw ExceptionUtils.getRootCause(bce);
        }
    }




}
