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
package org.neo4j.rest.graphdb.util;

/**
 * @author mh
 * @since 13.12.10
 */

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.rest.graphdb.PropertiesMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StreamJsonHelper {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap( InputStream stream ) {
        return (Map<String, Object>) readJson( stream );
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> jsonToListOfRelationshipRepresentations( InputStream stream ) {
        return (List<Map<String, Object>>) readJson( stream );
    }

    public static Object readJson( InputStream stream ) {
        try {
            return OBJECT_MAPPER.readValue(stream, Object.class);
        } catch ( IOException e ) {
            throw new RuntimeException( "Error reading input '"+JsonHelper.readString(stream)+"' as JSON ", e);
        } finally {
            if (stream!=null) {
                close(stream);
            }
        }
    }

    private static void close(InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static Object jsonToSingleValue( InputStream stream ) {
        Object jsonObject = readJson( stream );
        return jsonObject;
/*        return jsonObject instanceof Collection<?> ? jsonObject :
                PropertiesMap.assertSupportedPropertyValue( jsonObject );
*/
    }

    // todo boolean close
    public static void writeJsonTo( Object data , OutputStream stream) {
        try {
            JsonGenerator generator = OBJECT_MAPPER.getJsonFactory()
                    .createJsonGenerator(stream);
            OBJECT_MAPPER.writeValue(generator, data);
            stream.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}