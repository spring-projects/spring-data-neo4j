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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.rest.graphdb.PropertiesMap;

public class JsonHelper {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap( String json ) {
        return (Map<String, Object>) readJson( json );
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> jsonToListOfRelationshipRepresentations( String json ) {
        return (List<Map<String, Object>>) readJson( json );
    }

    public static Object readJson( String json ) {
        try {
            return OBJECT_MAPPER.readValue( json, Object.class );
        } catch ( IOException e ) {
            throw new RuntimeException( "Error reading as JSON '"+json+"'", e);
        }
    }

    public static Object jsonToSingleValue( String json ) {
        Object jsonObject = readJson( json );
        return jsonObject;
/*
        return jsonObject instanceof Collection<?> ? jsonObject :
                PropertiesMap.assertSupportedPropertyValue( jsonObject );
*/
    }

    public static String createJsonFrom( Object data ) {
        try {
            StringWriter writer = new StringWriter();
            JsonGenerator generator = OBJECT_MAPPER.getJsonFactory()
                    .createJsonGenerator( writer ).useDefaultPrettyPrinter();
            OBJECT_MAPPER.writeValue(generator, data);
            writer.close();
            return writer.getBuffer().toString();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public static String readString(InputStream stream) {
        try {
            return new Scanner(stream).useDelimiter("\\Z").next();
        } catch(Exception ioe) {
            System.err.println("Error reading string from stream "+ioe.getMessage());
            return "";
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}