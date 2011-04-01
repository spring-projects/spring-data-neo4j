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

package org.neo4j.rest.graphdb.util;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 02.02.11
 */
public class ArrayConverter {
        public Object toArray(Collection col) {
        Object entry = getNonNullEntry(col);
        if (entry==null) return null;
        Class<? extends Object> elementClass = getArrayElementClass( entry );
        Object array = Array.newInstance(elementClass, col.size());
        if (Object.class.isAssignableFrom( elementClass)) {
            col.toArray( (Object[])array );
        } else {
            int i=0;
            for ( Object value : col ) {
                setArrayValue(array,i,value,elementClass);
                i+=1;
            }
        }
        return array;
    }

    private void setArrayValue( Object array, int i, Object value, Class<? extends Object> type ) {
        if (value==null) return;
        if ( value instanceof Number ) {
            Number number = (Number) value;
            if (type.equals( int.class )) { Array.setInt( array, i, number.intValue()); return;}
            if (type.equals( long.class )) { Array.setLong( array, i, number.longValue());  return;}
            if (type.equals( double.class )) { Array.setDouble( array, i, number.doubleValue());  return;}
            if (type.equals( float.class )) { Array.setFloat( array, i, number.floatValue()); return;}
            if (type.equals( byte.class )) { Array.setByte( array, i, number.byteValue());  return;}
            if (type.equals( short.class )) { Array.setShort( array, i, number.shortValue());  return;}
        }
        if (type.equals( char.class )) { Array.setChar( array, i, (Character)value );  return;}
        if (type.equals( boolean.class )) { Array.setBoolean( array, i, (Boolean) value );  return;}
    }

    private Class<? extends Object> getArrayElementClass( Object entry ) {
        Class<? extends Object> type = entry.getClass();
        if (type.equals( Integer.class )) return int.class;
        if (type.equals( Long.class )) return long.class;
        if (type.equals( Double.class )) return double.class;
        if (type.equals( Float.class )) return float.class;
        if (type.equals( Byte.class )) return byte.class;
        if (type.equals( Short.class )) return short.class;
        if (type.equals( Character.class )) return char.class;
        if (type.equals( Boolean.class )) return boolean.class;
        return type;
    }

    private Object getNonNullEntry(Collection col) {
        for ( Object entry : col ) {
            if (entry!=null) return entry;
        }
        return null;
    }
}
