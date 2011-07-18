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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RestRequest {

    public static final int CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    public static final int READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    private final URI baseUri;
    private final Client client;

    public RestRequest( URI baseUri ) {
        this( baseUri, null, null );
    }

    public RestRequest( URI baseUri, String username, String password ) {
        this.baseUri = uriWithoutSlash( baseUri );
        client = createClient();
        addAuthFilter(username, password);

    }

    private void addAuthFilter(String username, String password) {
        if (username == null) return;
        client.addFilter( new HTTPBasicAuthFilter( username, password ) );
    }

    private Client createClient() {
        Client client = Client.create();

        client.setConnectTimeout(CONNECT_TIMEOUT);
        client.setReadTimeout(READ_TIMEOUT);

        return client;
    }

    private RestRequest( URI uri, Client client ) {
        this.baseUri = uriWithoutSlash( uri );
        this.client = client;
    }

    private URI uriWithoutSlash( URI uri ) {
        String uriString = uri.toString();
        return uriString.endsWith( "/" ) ? uri( uriString.substring( 0, uriString.length() - 1 ) ) : uri;
    }

    public static String encode( Object value ) {
        if ( value == null ) return "";
        try {
            return URLEncoder.encode( value.toString(), "utf-8" ).replaceAll( "\\+", "%20" );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }


    private Builder builder( String path ) {
        WebResource resource = client.resource( uri( pathOrAbsolute( path ) ) );
        return resource.accept( MediaType.APPLICATION_JSON_TYPE );
    }

    private String pathOrAbsolute( String path ) {
        if ( path.startsWith( "http://" ) ) return path;
        return baseUri + "/" + path;
    }

    public RequestResult get( String path ) {
        return RequestResult.extractFrom(builder(path).get(ClientResponse.class));
    }

    public RequestResult get( String path, String data ) {
        Builder builder = builder(path);
        if ( data != null ) {
            builder = builder.entity( data, MediaType.APPLICATION_JSON_TYPE );
        }
        return RequestResult.extractFrom(builder.get(ClientResponse.class));
    }

    public RequestResult delete(String path) {
        return RequestResult.extractFrom(builder(path).delete(ClientResponse.class));
    }

    public RequestResult post( String path, String data ) {
        Builder builder = builder( path );
        if ( data != null ) {
            builder = builder.entity( data, MediaType.APPLICATION_JSON_TYPE );
        }
        return RequestResult.extractFrom(builder.post(ClientResponse.class));
    }

    public void put( String path, String data ) {
        Builder builder = builder( path );
        if ( data != null ) {
            builder = builder.entity( data, MediaType.APPLICATION_JSON_TYPE );
        }
        final ClientResponse response = builder.put(ClientResponse.class);
        response.close();
    }


    public Object toEntity( RequestResult requestResult) {
        return JsonHelper.jsonToSingleValue( entityString(requestResult) );
    }

    public Map<?, ?> toMap( RequestResult requestResult) {
        final String json = entityString(requestResult);
        return JsonHelper.jsonToMap(json);
    }

    private String entityString( RequestResult requestResult) {
        return requestResult.getEntity();
    }

    public boolean statusIs( RequestResult requestResult, Response.StatusType status ) {
        return requestResult.getStatus() == status.getStatusCode();
    }

    public boolean statusOtherThan( RequestResult requestResult, Response.StatusType status ) {
        return !statusIs(requestResult, status );
    }

    public RestRequest with( String uri ) {
        return new RestRequest( uri( uri ), client );
    }

    private URI uri( String uri ) {
        try {
            return new URI( uri );
        } catch ( URISyntaxException e ) {
            throw new RuntimeException( e );
        }
    }

    public URI getUri() {
        return baseUri;
    }
}
