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

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.converter.TypeInformation;

/**
 * User: KBurchardi
 * Date: 18.10.11
 * Time: 16:11
 */
public class TypeInformationTest {

    @Test
    public void testSingleValueNode(){
        TypeInformation typeInfo = createTypeInfo("testSingleValueNode");
        assertEquals(Node.class, typeInfo.getType());
    }

    @Test
    public void testSingleValueString(){
        TypeInformation typeInfo = createTypeInfo("testSingleValueString");
        assertEquals(String.class, typeInfo.getType());
    }

    @Test
    public void testIterableObject(){
        TypeInformation typeInfo = createTypeInfo("testIterableObject");
        assertEquals(Iterable.class, typeInfo.getType());
        assertEquals(1, typeInfo.getGenericArguments().length);
        assertEquals(Object.class, typeInfo.getGenericArguments()[0]);
    }

    @Test
    public void testIterableNode(){
        TypeInformation typeInfo = createTypeInfo("testIterableNode");
        assertEquals(Iterable.class, typeInfo.getType());
        assertEquals(1, typeInfo.getGenericArguments().length);
        assertEquals(Node.class, typeInfo.getGenericArguments()[0]);
    }

    @Test
    public void testCollectionObject(){
        TypeInformation typeInfo = createTypeInfo("testCollectionObject");
        assertEquals(Collection.class, typeInfo.getType());
        assertEquals(1, typeInfo.getGenericArguments().length);
        assertEquals(Object.class, typeInfo.getGenericArguments()[0]);
    }

    @Test
    public void testCollectionNode(){
        TypeInformation typeInfo = createTypeInfo("testCollectionNode");
        assertEquals(Collection.class, typeInfo.getType());
        assertEquals(1, typeInfo.getGenericArguments().length);
        assertEquals(Node.class, typeInfo.getGenericArguments()[0]);
    }

    @Test
    public void testMapStringObject(){
        TypeInformation typeInfo = createTypeInfo("testMapStringObject");
        assertEquals(Map.class, typeInfo.getType());
        assertEquals(2, typeInfo.getGenericArguments().length);
        assertEquals(String.class, typeInfo.getGenericArguments()[0]);
        assertEquals(Object.class, typeInfo.getGenericArguments()[1]);
    }

    @Test
    public void testMapIntegerNode(){
        TypeInformation typeInfo = createTypeInfo("testMapIntegerNode");
        assertEquals(Map.class, typeInfo.getType());
        assertEquals(2, typeInfo.getGenericArguments().length);
        assertEquals(Integer.class, typeInfo.getGenericArguments()[0]);
        assertEquals(Node.class, typeInfo.getGenericArguments()[1]);
    }

    @Test
    public void testTypeInformationBasicMethods(){
        TypeInformation typeInfoBasic = createTypeInfo("testSingleValueNode");
        assertTrue(typeInfoBasic.isInstance("test", String.class));
        assertTrue(typeInfoBasic.isSingleType());
        assertTrue(typeInfoBasic.isGraphEntity(Node.class));

        TypeInformation typeInfoCollection = createTypeInfo("testCollectionObject");
        assertTrue(typeInfoCollection.isCollectionType());
        assertTrue(typeInfoCollection.isCollection());

        TypeInformation typeInfoMap = createTypeInfo("testMapStringObject");
        assertTrue(typeInfoMap.isCollectionType());
        assertTrue(typeInfoMap.isMap());
    }

    @Test
    public void testCreateTypeInformationByIterable(){
        TypeInformation typeInfoIterable = new TypeInformation(asList("test","test2"));
        assertTrue(typeInfoIterable.isCollectionType());
        assertTrue(typeInfoIterable.isCollection());
        assertEquals(1, typeInfoIterable.getGenericArguments().length);
        assertEquals(String.class, typeInfoIterable.getGenericArguments()[0]);
    }

    @Test
    public void testCreateTypeInformationByMap(){
        TypeInformation typeInfoMap = new TypeInformation(MapUtil.map("test",1));
        assertTrue(typeInfoMap.isCollectionType());
        assertTrue(typeInfoMap.isMap());
        assertEquals(2, typeInfoMap.getGenericArguments().length);
        assertEquals(String.class, typeInfoMap.getGenericArguments()[0]);
        assertEquals(Integer.class, typeInfoMap.getGenericArguments()[1]);
    }



    public TypeInformation createTypeInfo(String methodName){
          try {
            return new TypeInformation(TypeInformationTestInterface.class.getMethod(methodName).getGenericReturnType());
          } catch (NoSuchMethodException e) {
            return null;
          }
    }
}
