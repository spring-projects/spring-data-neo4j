/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.unit.authentication;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.authentication.CredentialsService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class CredentialsServiceTest {

    @Before
    public void setUp() {
        System.setProperty("username", "neo4j");
        System.setProperty("password", "password");
    }

    @After
    public void tearDown() {
        System.getProperties().remove("username");
        System.getProperties().remove("password");
    }

    @Test
    public void testUserNameAndPassword() {
        assertEquals("bmVvNGo6cGFzc3dvcmQ=", CredentialsService.userNameAndPassword().credentials());

    }


    @Test
    public void testUserNameNoPassword() {

        System.getProperties().remove("password");
        assertNull(CredentialsService.userNameAndPassword());

    }


    @Test
    public void testNoUserNamePassword() {

        System.getProperties().remove("username");
        assertNull(CredentialsService.userNameAndPassword());

    }

}
