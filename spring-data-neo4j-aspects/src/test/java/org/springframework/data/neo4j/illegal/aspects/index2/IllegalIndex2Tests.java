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

    @Test
    @Transactional
    public void indexAccessWithFullAndNoIndexNameShouldFail() {

        try {
            // This no longer blows up at access time, but rather at startup
            ClassPathXmlApplicationContext appCtx = new ClassPathXmlApplicationContext(
                "org/springframework/data/neo4j/aspects/support/illegal-index2-tests-context.xml");
        } catch (BeanCreationException bce) {
            Throwable t =  bce.getCause().getCause().getCause().getCause();
            assertEquals("unexpected underlying cause",
                    IllegalStateException.class, t.getClass());
            return;
        }

        fail("Should never get here ...");
        //InvalidIndexed invalidIndexed = persist(new InvalidIndexed());
        //invalidIndexed.setFulltextNoIndexName(NAME_VALUE);
    }


}
