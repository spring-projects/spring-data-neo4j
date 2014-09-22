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
