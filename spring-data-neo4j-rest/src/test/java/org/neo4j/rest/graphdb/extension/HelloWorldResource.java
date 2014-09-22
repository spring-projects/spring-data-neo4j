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
package org.neo4j.rest.graphdb.extension;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/helloworld")
public class HelloWorldResource {

    @GET
    @Path("/{nodeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("nodeId") long nodeId) {
        return Response.status(Status.OK).entity(("\"get " + nodeId + "\"").getBytes()).build();
    }
    @PUT
    @Path("/{nodeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("nodeId") long nodeId, String body) {
        return Response.status(Status.OK).entity(("\"put " + nodeId +":"+body +"\"").getBytes()).build();
    }
    @POST
    @Path("/{nodeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(@PathParam("nodeId") long nodeId, String body) {
        return Response.status(Status.OK).entity(("\"post " + nodeId +":"+body + "\"").getBytes()).build();
    }
    @POST
    @Path("/empty/{nodeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postWithoutResult(@PathParam("nodeId") long nodeId) {
        return Response.status(Status.NO_CONTENT).build();
    }
    @DELETE
    @Path("/{nodeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("nodeId") long nodeId) {
        return Response.status(Status.OK).entity(("\"delete " + nodeId + "\"").getBytes()).build();
    }
}