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

package org.springframework.data.graph.neo4j.rest.support;

/**
 * @author mh
 * @since 13.12.10
 */

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    private static Object readJson( String json ) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue( json, Object.class );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public static Object jsonToSingleValue( String json ) {
        Object jsonObject = readJson( json );
        return jsonObject instanceof Collection<?> ? jsonObject :
                PropertiesMap.assertSupportedPropertyValue( jsonObject );
    }

    public static String createJsonFrom( Object data ) {
        try {
            StringWriter writer = new StringWriter();
            JsonGenerator generator = OBJECT_MAPPER.getJsonFactory()
                    .createJsonGenerator( writer ).useDefaultPrettyPrinter();
            OBJECT_MAPPER.writeValue( generator, data );
            writer.close();
            return writer.getBuffer().toString();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}