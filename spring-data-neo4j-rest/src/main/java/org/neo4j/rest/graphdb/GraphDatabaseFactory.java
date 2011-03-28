package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author mh
 * @since 25.01.11
 */
public class GraphDatabaseFactory {
    public static GraphDatabaseService databaseFor(String url) {
        return databaseFor( url, null,null );
    }

    public static GraphDatabaseService databaseFor(String url, String username, String password) {
        if (url.startsWith( "http://" ) || url.startsWith( "https://" )) {
            return new RestGraphDatabase( toURI( url ), username,password );
        }
        String path=url;
        if (url.startsWith( "file:" )) {
            path = toURI( url ).getPath();
        }
        File file = new File( path );
        if (!file.isDirectory()) file=file.getParentFile();
        return new EmbeddedGraphDatabase( file.getAbsolutePath() );
    }

    private static URI toURI( String uri ) {
        try {
            return new URI(uri);
        } catch ( URISyntaxException e ) {
            throw new RuntimeException( "Error using URI "+uri, e);
        }
    }
}
