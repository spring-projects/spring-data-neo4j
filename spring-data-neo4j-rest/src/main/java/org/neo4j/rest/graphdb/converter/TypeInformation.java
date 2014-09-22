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
package org.neo4j.rest.graphdb.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

/**
* User: KBurchardi
* Date: 18.10.11
* Time: 17:16
*/
public class TypeInformation {

    Class type;
    Class[] genericArguments;

    public TypeInformation(Type type) {
        this.type = convertToClass(type);
        this.genericArguments = extractGenericArguments(type);
    }

    public TypeInformation(Object object){
        this.type = object.getClass();
        this.genericArguments = extractGenericArgumentsFromObject(object);
    }

    private Class convertToClass(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        }else{
            return(Class)((ParameterizedType)type).getRawType();
        }
    }

    public boolean isSingleType(){
        return !isCollectionType();
    }

    public boolean isCollectionType() {
        return (isCollection() || isMap());
    }

    public boolean isMap() {
        return Map.class.isAssignableFrom(this.type);
    }

    public boolean isCollection() {
        return Iterable.class.isAssignableFrom(this.type);
    }

    private Class[] extractGenericArgumentsFromObject(Object object){
        if (isCollectionType()){
           if(isCollection()){
              if(((Iterable)object).iterator().hasNext()){
                 return  new Class[]{((Iterable)object).iterator().next().getClass()};
              }
           }else{
              if (!((Map)object).isEmpty()){
                  return  new Class[]{((Map)object).keySet().iterator().next().getClass(), ((Map)object).values().iterator().next().getClass()};
              }
           }
        }
        return null;
    }




    private Class[] extractGenericArguments(Type type) {

        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        Class[]result = new Class[actualTypeArguments.length];
        for (int i = 0; i < actualTypeArguments.length; i++) {
            result[i] = convertToClass(actualTypeArguments[i]);
        }
        return result;
    }

    public boolean isInstance(Object resultObject, Class type) {
        return type.isInstance(resultObject);
    }

    public boolean isGraphEntity(Class classType) {
        return Node.class.isAssignableFrom(classType)|| Relationship.class.isAssignableFrom(classType);
    }

    public boolean isPath(Class classType){
        return Path.class.isAssignableFrom(classType);
    }

     public Class getType() {
        return type;
    }

    public Class[] getGenericArguments() {
        return genericArguments;
    }
}
