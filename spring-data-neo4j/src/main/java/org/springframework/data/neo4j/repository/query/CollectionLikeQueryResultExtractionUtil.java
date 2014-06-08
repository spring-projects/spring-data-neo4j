package org.springframework.data.neo4j.repository.query;

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.conversion.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Utility which understands how to extract collection like results returned
 * from a query into a format which is expected by the caller.
 *
 * @author Nicki Watt
 * @since 08.06.201
 */
public class CollectionLikeQueryResultExtractionUtil {

    public static Object extractCollectionLikeResult(Result<?> initialResult,Class<?> targetType, Class<?> collectionLikeType) {
        final Result<?> result = initialResult.to(targetType);
        if (isSetResult(collectionLikeType)) return IteratorUtil.addToCollection(result, new LinkedHashSet());
        if (isCollectionResult(collectionLikeType)) return IteratorUtil.addToCollection(result,new ArrayList());
        return result;
    }

    private static  boolean isSetResult(Class<?> collectionLikeType) {
        return hasResultOfType(Set.class,collectionLikeType);
    }

    private static boolean hasResultOfType(Class<?> superClass,Class<?> collectionLikeType) {
        return superClass.isAssignableFrom(collectionLikeType);
    }

    private static boolean isCollectionResult(Class<?> collectionLikeType) {
        return hasResultOfType(Collection.class,collectionLikeType);
    }

}