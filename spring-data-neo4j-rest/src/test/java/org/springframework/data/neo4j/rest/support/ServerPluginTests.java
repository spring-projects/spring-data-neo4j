/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest.support;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.rest.graphdb.RequestResult;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.server.ProvidedClassPathXmlApplicationContext;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

/**
 * @author mh
 * @since 14.04.11
 */
public class ServerPluginTests extends RestTestBase {

    @BeforeClass
    public static void init() {
        new ProvidedClassPathXmlApplicationContext(db, "Plugin-context.xml");
    }

    @Test
    public void testGetFriends() throws IOException {
        Person person = persistedPerson("Michael", 35);
        final RequestResult requestResult = RequestResult.extractFrom(createRequest("ext/TestServerPlugin/graphdb/person").post(ClientResponse.class, "{\"name\":\"" + person.getName() + "\"}"));
        assertEquals(200, requestResult.getStatus());
        final String result = requestResult.getText();
        final Map data = (Map) new ObjectMapper().readValue(result, Object.class);
        assertEquals(person.getName(),((Map)data.get("data")).get("name"));

    }

    private ClientResponse post(String uriSuffix, String params) {
        return createRequest(uriSuffix).post(ClientResponse.class, params);
    }

    private static WebResource.Builder createRequest(String uriSuffix) {
        return Client.create().
                    resource(SERVER_ROOT_URI + uriSuffix).
                    type(MediaType.APPLICATION_JSON).
                    accept(MediaType.APPLICATION_JSON);
    }
}
