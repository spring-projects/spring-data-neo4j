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

package org.springframework.data.neo4j.rest;

import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
* @author mh
* @since 27.06.11
*/
public class RequestResult {
    private final int status;
    private final URI location;
    private final String entity;

    RequestResult(int status, URI location, String entity) {
        this.status = status;
        this.location = location;
        this.entity = entity;
    }

    public static RequestResult extractFrom(ClientResponse clientResponse) {
        final int status = clientResponse.getStatus();
        final URI location = clientResponse.getLocation();
		final String data = status != Response.Status.NO_CONTENT.getStatusCode() ? clientResponse.getEntity(String.class) : null;
        clientResponse.close();
        return new RequestResult(status, location, data);
    }

    public int getStatus() {
        return status;
    }

    public URI getLocation() {
        return location;
    }

    public String getEntity() {
        return entity;
    }
}
