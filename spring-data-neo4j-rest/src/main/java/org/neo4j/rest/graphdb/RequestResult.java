/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.neo4j.rest.graphdb.util.JsonHelper;

import com.sun.jersey.api.client.ClientResponse;
import org.neo4j.rest.graphdb.util.StreamJsonHelper;


/**
* @author Klemens Burchardi
* @since 03.08.11
*/
public class RequestResult {
    private final int status;
    private final String location;
    private ClientResponse response;
    private String string;
    private Object entity;
    private InputStream stream;

    RequestResult(int status, String location, String string) {
        this.status = status;
        this.location = location;
        this.string = string;
        this.stream = null;
    }

    RequestResult(int status, String location, InputStream stream, ClientResponse response) {
        this.status = status;
        this.location = location;
        this.response = response;
        this.entity = null;
        this.stream = stream;
    }

    public static RequestResult extractFrom(ClientResponse clientResponse) {
        final int status = clientResponse.getStatus();
        final URI location = clientResponse.getLocation();
        // final InputStream data;
        if (status == Response.Status.NO_CONTENT.getStatusCode()) {
        //    data = null;
            clientResponse.close();
            return new RequestResult(status, uriString(location), null,clientResponse);
        } else {
        //    data = clientResponse.getEntityInputStream();
            RequestResult result = new RequestResult(status, uriString(location), clientResponse.getEntity(String.class));
            clientResponse.close();
            return result;
        }
        //return new RequestResult(status, uriString(location), data,clientResponse);
    }

    public static RequestResult extractFrom(Map<String, Object> batchResult) {
        return new RequestResult(200, (String) batchResult.get("location"),JsonHelper.createJsonFrom(batchResult.get("body")));
    }

    private static String uriString(URI location) {
        return location==null ? null : location.toString();
    }


    public int getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    public Object toEntity() {
        if (entity!=null) return entity;
        if (stream != null) {
            entity = StreamJsonHelper.jsonToSingleValue(stream);
            closeStream();
        }
        else {
            entity = JsonHelper.jsonToSingleValue(string);
        }
        return entity;
    }

    public boolean isMap() {
        return toEntity() instanceof Map;
    }
    public Map<?, ?> toMap() {
        return (Map<?, ?>) toEntity();
    }

    public boolean statusIs( StatusType status ) {
        return getStatus() == status.getStatusCode();
    }

    public boolean statusOtherThan( StatusType status ) {
        return !statusIs(status );
    }

    public String getText() {
        if (string==null && stream!=null) {
            string = JsonHelper.readString(stream);
            closeStream();
        }
        return string;
    }

    private void closeStream() {
        if (stream!=null) readFully(stream);
        stream = null;
        if (response!=null) {
            response.close();
            response = null;
        }
    }

    private void readFully(InputStream stream) {
        try {
            while (stream.read()!=-1);
        } catch (IOException e) {
            // ignore
        }
    }
}
