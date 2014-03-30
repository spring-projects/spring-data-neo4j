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

import org.springframework.data.geo.*;
import org.springframework.data.geo.Shape;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;

/**
 * Repository for spatial queries.
 *
 * WKT is well known text format like POINT( LON LAT ) POLYGON (( LON1 LAT1 LON2 LAT2 LON3 LAT3 LON1 LAT1 ))
 *
 * Right now requires a field: @Indexed(type = POINT, indexName = "...") String wkt;
 * inside the entity.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Well-known_text">Well Known Text Spatial Format</a>
 */
public interface SpatialRepository<T> {
    @Transactional
    EndResult<T> findWithinBoundingBox(String indexName, double lowerLeftLat,
                                              double lowerLeftLon,
                                              double upperRightLat,
                                              double upperRightLon);

    @Transactional
    EndResult<T> findWithinBoundingBox(String indexName, Box box);

    @Transactional
    EndResult<T> findWithinDistance( final String indexName, final double lat, double lon, double distanceKm);

    @Transactional
    EndResult<T> findWithinDistance( final String indexName, Circle circle);

    @Transactional
    EndResult<T> findWithinWellKnownText( final String indexName, String wellKnownText);

    /**
     * Converts the shape into a well-known text representation and executes the appropriate WKT query
     */
    @Transactional
    EndResult<T> findWithinShape( final String indexName, Shape shape);
}

