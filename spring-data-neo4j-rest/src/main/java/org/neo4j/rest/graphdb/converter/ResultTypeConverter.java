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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestResultException;
import org.neo4j.rest.graphdb.traversal.RestPathParser;

/**
 * User: KBurchardi
 * Date: 18.10.11
 * Time: 17:55
 */
public class ResultTypeConverter {

    private RestAPI restAPI;
    private RestEntityExtractor restEntityExtractor;

    public ResultTypeConverter(RestAPI restAPI) {
        this.restAPI = restAPI;
        this.restEntityExtractor = new RestEntityExtractor(this.restAPI);
    }

    public Object convertToResultType(Object resultObject, TypeInformation typeInformation){
        if (typeInformation.isSingleType() || typeInformation.isPath(typeInformation.type)) {
            return convertSingleTypeToResultType(resultObject, typeInformation, typeInformation.type);
        }
        if (typeInformation.isCollection()){
            ArrayList<Object> result = new ArrayList<Object>();
            for (Object innerObject : toIterable(resultObject)) {
                result.add(convertSingleTypeToResultType(innerObject, typeInformation, typeInformation.genericArguments[0]));
            }
            return result;
        }if (typeInformation.isMap()){
            Map<?,?> originalMap =  toMap(resultObject);
            HashMap<Object,Object> result = new HashMap<Object, Object>(((Map)resultObject).size());
            for (Map.Entry<?,?> entry : originalMap.entrySet()) {
                Object resultKey = convertSingleTypeToResultType(entry.getKey(), typeInformation, typeInformation.genericArguments[0]);
                Object resultValue = convertSingleTypeToResultType(entry.getValue(), typeInformation, typeInformation.genericArguments[1]);
                result.put(resultKey, resultValue);
            }
            return result;
        }
           throw new RestResultException("could not convert Type "+ resultObject.getClass().getName()+" to Type "+typeInformation.type.getName());

    }

    private Iterable<?> toIterable(Object resultObject){
        if (Iterable.class.isAssignableFrom(resultObject.getClass())){
            return (Iterable<?>)resultObject;
        }else{
           final RestTableResultExtractor extractor = new RestTableResultExtractor(new RestEntityExtractor(this.restAPI));
           if (extractor.canHandle(resultObject)){
              final List<Map<String, Object>> data = extractor.extract((Map)resultObject);
               return (Iterable<?>)data;
           }
        }
         throw new RestResultException("could not convert Type "+ resultObject.getClass().getName()+" to Iterable");
    }

    private Map<?,?> toMap (Object resultObject){
       if (Map.class.isAssignableFrom(resultObject.getClass())){
            return (Map<?,?>)resultObject;
        }
         throw new RestResultException("could not convert Type "+ resultObject.getClass().getName()+" to Map");
    }



    private Object convertSingleTypeToResultType(Object resultObject, TypeInformation typeInformation, Class singleObjectType){
          if (typeInformation.isInstance(resultObject, singleObjectType)){
                return resultObject;
          }else{
               if (typeInformation.isGraphEntity(singleObjectType)){
                   if (restEntityExtractor.canHandle(resultObject)){
                       return restEntityExtractor.convertFromRepresentation(resultObject);
                   }
               }

              if (typeInformation.isPath(singleObjectType) && resultObject instanceof Map){
                  return RestPathParser.parse((Map)resultObject, restAPI);
              }

               ConversionInfo resultInfo = convertFromCollectionType(resultObject, singleObjectType);
               if (resultInfo.isSuccessfulConversion()){
                   return resultInfo.getConversionData();
               }
          }
         throw new RestResultException("could not convert Type "+ resultObject.getClass().getName()+" to Type "+typeInformation.type.getName());
    }

    private ConversionInfo convertFromCollectionType(Object resultObject, Class singleObjectType){
        TypeInformation resultTypeInfo = new TypeInformation(resultObject);

        if (resultTypeInfo.isCollectionType()){
            if (resultTypeInfo.isCollection()){
              Iterable<?> resultIterable = (Iterable) resultObject;
              if(!resultIterable.iterator().hasNext()){
                  return new ConversionInfo(null,true);
              }else{
                  if (iterableHasSingleElement(resultIterable) && resultTypeInfo.getGenericArguments()[0].equals(singleObjectType)){
                      return new ConversionInfo(resultIterable.iterator().next(), true);
                  }
              }
            }else{
              Map<?,?> resultMap = (Map)resultObject;
              if (resultMap.isEmpty()){
                  return new ConversionInfo(null,true);
              }else{
                 if (resultMap.size() == 1 && resultTypeInfo.getGenericArguments()[1].equals(singleObjectType) ){
                    return new ConversionInfo(resultMap.values().iterator().next(), true);
                 }
              }
            }
        }
        return new ConversionInfo(null,false);
    }

    public boolean iterableHasSingleElement(Iterable<?> object){
        Iterator<?> it = object.iterator();
        if (!it.hasNext()){
            return false;
        }

        it.next();
        if (it.hasNext()){
            return false;
        }
        return true;

    }
}
