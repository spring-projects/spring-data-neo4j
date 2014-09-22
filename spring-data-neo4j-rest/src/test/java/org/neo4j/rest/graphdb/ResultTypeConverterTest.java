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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.converter.ResultTypeConverter;
import org.neo4j.rest.graphdb.converter.TypeInformation;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.traversal.SimplePath;

/**
 * User: KBurchardi
 * Date: 18.10.11
 * Time: 18:13
 */
public class ResultTypeConverterTest extends RestTestBase {

    private ResultTypeConverter converter;
    private RestAPI restAPI;

    @Before
    public void init(){
        restAPI = ((RestGraphDatabase)getRestGraphDb()).getRestAPI();
        converter = new ResultTypeConverter(restAPI);
    }


    @Test
    public void testConvertJSONDataToNode(){
        Object result = converter.convertToResultType(MapUtil.map("self","http://localhost:7474/db/data/node/2", "data", MapUtil.map("propname", "testprop")), new TypeInformation(RestNode.class));
        assertEquals(RestNode.class, result.getClass());
        assertEquals("testprop", ((Node)result).getProperty("propname"));
    }

    @Test
    public void testConvertJSONDataToRelationship(){
        Object result = converter.convertToResultType(MapUtil.map("self","http://localhost:7474/db/data/relationship/2", "data", MapUtil.map("propname", "testprop")), new TypeInformation(RestRelationship.class));
        assertEquals(RestRelationship.class, result.getClass());
        assertEquals("testprop", ((Relationship)result).getProperty("propname"));
    }


     @Test
    public void testConvertJSONDataToPath(){
        String node1 = "http://localhost:7474/db/data/node/1";
        String node2 = "http://localhost:7474/db/data/node/2";
        String relationship1 = "http://localhost:7474/db/data/relationship/1";
        Map<String, Object> path = new HashMap<String, Object>();
        path.put("start", node1);
        path.put("nodes", asList(node1, node2));
        path.put("length",1);
        path.put("relationships", asList(relationship1));
        path.put("end", node2);
        Path result = (Path)converter.convertToResultType(path, new TypeInformation(Path.class));

        assertEquals(SimplePath.class, result.getClass());
        assertEquals(1, result.startNode().getId());
        assertEquals(2, result.endNode().getId());
        assertEquals(1, result.lastRelationship().getId());
    }


    @Test
    public void testConvertJSONDataToFullPath(){
        Map<String, Object> node1 = MapUtil.map("self","http://localhost:7474/db/data/node/1", "data", MapUtil.map("propname", "testprop1"));
        Map<String, Object> node2 = MapUtil.map("self","http://localhost:7474/db/data/node/2", "data", MapUtil.map("propname", "testprop2"));
        Map<String, Object> relationship1 = MapUtil.map("self","http://localhost:7474/db/data/relationship/1", "data", MapUtil.map("propname", "testproprel1"));
        Map<String, Object> path = new HashMap<String, Object>();
        path.put("start", node1);
        path.put("nodes", asList(node1, node2));
        path.put("length",1);
        path.put("relationships", asList(relationship1));
        path.put("end", node2);
        Object result = converter.convertToResultType(path, new TypeInformation(Path.class));
        assertEquals(SimplePath.class, result.getClass());
        assertEquals("testprop1",  ((SimplePath)result).startNode().getProperty("propname"));
        assertEquals("testprop2",  ((SimplePath)result).endNode().getProperty("propname"));
        assertEquals("testproprel1",  ((SimplePath)result).lastRelationship().getProperty("propname"));

    }

    @Test
    public void testConvertSimpleObjectToSameClass(){
        Object result = converter.convertToResultType("test", new TypeInformation(String.class));
        assertEquals(String.class, result.getClass());
        assertEquals("test", result);
    }

    @Test
    public void testConvertIterableToIterableWithSameType(){
        Object result = converter.convertToResultType(asList("test","test2"), new TypeInformation(asList("test")));
        assertEquals(asList("test","test2"), result);
    }

    @Test (expected = RestResultException.class)
    public void testConvertIterableToIterableWithWrongType(){
        converter.convertToResultType(asList("test"), new TypeInformation(asList(2)));
    }

    @Test
    public void testConvertMapToMapWithSameType(){
        Object result = converter.convertToResultType(MapUtil.map("test",1,"test2",2), new TypeInformation(MapUtil.map("test",0)));
        assertEquals(MapUtil.map("test", 1, "test2", 2), result);
    }

    @Test (expected = RestResultException.class)
    public void testConvertMapToMapWithWrongType(){
        converter.convertToResultType(MapUtil.map("test",1,"test2",2), new TypeInformation(MapUtil.map("test","0")));
    }

    @Test (expected = RestResultException.class)
    public void testConvertSimpleObjectToWrongClass(){
         converter.convertToResultType("test", new TypeInformation(Integer.class));
    }

    @Test
    public void testConvertDifferentIterablesWithSameType(){
        HashSet<String> set = new HashSet<String>();
        set.add("test");
        Object result = converter.convertToResultType(set, new TypeInformation(asList("t")));
        assertEquals(asList("test"), result);
    }

    @Test (expected = RestResultException.class)
    public void testConvertDifferentIterablesWithWrongType(){
        HashSet<String> set = new HashSet<String>();
        set.add("test");
        converter.convertToResultType(set, new TypeInformation(asList(2)));
    }


    @Test
    public void testConvertDifferentMapsWithSameType(){
        Hashtable<String,String> table = new Hashtable<String,String>();
        table.put("testkey", "testvalue");
        Object result = converter.convertToResultType(table, new TypeInformation(MapUtil.map("test","test")));
        assertEquals(MapUtil.map("testkey","testvalue"), result);
    }

    @Test (expected = RestResultException.class)
    public void testConvertDifferentMapsWithWrongType(){
        Hashtable<String,String> table = new Hashtable<String,String>();
        table.put("testkey", "testvalue");
        converter.convertToResultType(table, new TypeInformation(MapUtil.map("test",2)));
    }

    @Test
    public void testConvertFromIterableWithSameTypeAndSingleElementToObject(){
        Object result = converter.convertToResultType(Collections.singletonList("test"), new TypeInformation(String.class));
        assertEquals(String.class, result.getClass());
        assertEquals("test", result);
    }

    @Test (expected = RestResultException.class)
    public void testConvertFromIterableWithWrongTypeToObject(){
        converter.convertToResultType(Collections.singletonList("test"), new TypeInformation(Integer.class));
    }

    @Test (expected = RestResultException.class)
    public void testConvertFromIterableWithSameTypeAndMultipleElementsToObject(){
        converter.convertToResultType(asList("test", "test2"), new TypeInformation(String.class));
    }

    @Test
    public void testConvertFromEmptyIterableToObject(){
        Object result = converter.convertToResultType(Collections.emptyList(), new TypeInformation(String.class));
        assertNull(result);
    }

    @Test
     public void testConvertFromMapWithSameTypeAndSingleElementToObject(){
        Object result = converter.convertToResultType(Collections.singletonMap("test", 2), new TypeInformation(Integer.class));
        assertEquals(Integer.class, result.getClass());
        assertEquals(2, result);
     }

    @Test
    public void testConvertFromEmptyMapToObject(){
        Object result = converter.convertToResultType(MapUtil.map(), new TypeInformation(String.class));
        assertNull(result);
    }

    @Test (expected = RestResultException.class)
    public void testConvertFromMapWithWrongTypeToObject(){
       converter.convertToResultType(MapUtil.map("test",2), new TypeInformation(String.class));
    }

    @Test (expected = RestResultException.class)
    public void testConvertFromMapWithSameTypeAndMultipleElementsToObject(){
        converter.convertToResultType(MapUtil.map("test","test","test2","test2"), new TypeInformation(String.class));
    }

    @Test
    public void testIterableHasSingleElement(){
        assertTrue(converter.iterableHasSingleElement(asList("test")));
        assertFalse(converter.iterableHasSingleElement(new ArrayList<Object>()));
        assertFalse(converter.iterableHasSingleElement(asList("test", "test2")));
    }
}
