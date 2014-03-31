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

package org.springframework.data.neo4j.repository;

import org.json.simple.JSONArray;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.Pair;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.geo.*;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;

import java.util.Map;

import static java.util.Arrays.asList;
import static org.neo4j.helpers.collection.MapUtil.map;

public class GeoQueries<S extends PropertyContainer, T> implements SpatialRepository<T> {
    public static final String WITHIN_WKT_GEOMETRY = "withinWKTGeometry";
    public static final String WITHIN_DISTANCE = "withinDistance";
    public static final String BBOX = "bbox";
    private final LegacyIndexSearcher<S,T> legacyIndexSearcher;

    public GeoQueries(LegacyIndexSearcher<S, T> legacyIndexSearcher) {
        this.legacyIndexSearcher = legacyIndexSearcher;
    }
    @Override
    public Result<T> findWithinWellKnownText(final String indexName, String wellKnownText) {
        return legacyIndexSearcher.geoQuery(indexName, WITHIN_WKT_GEOMETRY, wellKnownText);
    }

    @Override
    public Result<T> findWithinShape(String indexName, Shape shape) {
        Assert.notNull(indexName, "geo-index-name must not be null");
        Assert.notNull(shape,"shape must not be null");
        if (shape instanceof Circle) return findWithinDistance(indexName,(Circle)shape);
        if (shape instanceof Box) return findWithinBoundingBox(indexName, (Box) shape);
        if (shape instanceof Polygon) return findWithinWellKnownText(indexName, GeoConverter.toWkt((Polygon) shape));
        throw new InvalidDataAccessApiUsageException("Unknown shape "+shape.getClass().getSimpleName()+" "+shape);
    }

    @Override
    public Result<T> findWithinDistance(final String indexName, final double lat, double lon, double distanceKm) {
        return legacyIndexSearcher.geoQuery(indexName, WITHIN_DISTANCE, toWithinDistanceParams(lat, lon, distanceKm));
    }

    private static Map<String, Object> toWithinDistanceParams(double lat, double lon, double distanceKm) {
        return map("point", new Double[] { lat, lon}, "distanceInKm", distanceKm);
    }

    @Override
    public Result<T> findWithinDistance(String indexName, Circle circle) {
        return legacyIndexSearcher.geoQuery(indexName, WITHIN_DISTANCE, toWithinDistanceParams(circle));
    }

    private static Map<String, Object> toWithinDistanceParams(Circle circle) {
        double distance = circle.getRadius().in(Metrics.KILOMETERS).getValue();
        return toWithinDistanceParams(circle.getCenter().getY(),circle.getCenter().getX(),distance);
    }
    private static String toWithinDistanceParamsString(Circle circle) {
        double distance = circle.getRadius().in(Metrics.KILOMETERS).getValue();
        return JSONArray.toJSONString(asList(circle.getCenter().getY(), circle.getCenter().getX(), distance));
    }

    @Override
    public Result<T> findWithinBoundingBox(final String indexName, final double lowerLeftLat,
                                           final double lowerLeftLon, final double upperRightLat, final double upperRightLon) {
        return legacyIndexSearcher.geoQuery(indexName, BBOX, toBoundingBoxParams(lowerLeftLat, lowerLeftLon, upperRightLat, upperRightLon));
    }

    private static String toBoundingBoxParams(double lowerLeftLat, double lowerLeftLon, double upperRightLat, double upperRightLon) {
        return JSONArray.toJSONString(asList(lowerLeftLon, upperRightLon, lowerLeftLat, upperRightLat));
    }

    @Override
    public Result<T> findWithinBoundingBox(String indexName, Box box) {
        return legacyIndexSearcher.geoQuery(indexName,BBOX,toBoundingBoxParams(box));
    }

    private static String toBoundingBoxParams(Box box) {
        Point first = box.getFirst();
        Point second = box.getSecond();
        return toBoundingBoxParams(
                Math.min(first.getY(),second.getY()), Math.min(first.getX(),second.getX()),
                Math.max(first.getY(), second.getY()), Math.max(first.getX(), second.getX()));
    }

    public static Pair<String, String> toQueryParams(Part.Type type, Object value) {
        if (value instanceof String && type == Part.Type.WITHIN) return Pair.of(WITHIN_WKT_GEOMETRY,(String)value);
        if (value instanceof Circle && (type == Part.Type.NEAR || type == Part.Type.WITHIN)) return Pair.of(WITHIN_DISTANCE, toWithinDistanceParamsString((Circle) value));
        if (value instanceof Box && type == Part.Type.WITHIN) return Pair.of(BBOX, toBoundingBoxParams((Box)value));
        if (value instanceof Polygon && type == Part.Type.WITHIN) return Pair.of(WITHIN_WKT_GEOMETRY, GeoConverter.toWkt((Polygon) value));
        throw new IllegalArgumentException(
                String.format("Must have a geospatial operator like equals, within or inside, but has %s and must have a geospatial value like circle, box, polygon, or WKT-String but has %s %s",
                        type,
                        value==null ? null : value.getClass(),
                        value));
    }

}

/*
index.query( LayerNodeIndex.WITHIN_WKT_GEOMETRY_QUERY,
                "withinWKTGeometry:POLYGON ((15 56, 15 57, 16 57, 16 56, 15 56))" );

 hits = index.query( LayerNodeIndex.WITHIN_WKT_GEOMETRY_QUERY,
                 "POLYGON ((15 56, 15 57, 16 57, 16 56, 15 56))" ); lon,lat
         assertTrue( hits.hasNext() );
    final String poly = String.format("POLYGON (())", lowerLeftLon, upperRightLon, lowerLeftLat, upperRightLat);
 */
