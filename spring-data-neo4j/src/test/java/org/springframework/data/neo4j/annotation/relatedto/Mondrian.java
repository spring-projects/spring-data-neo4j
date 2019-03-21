/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.annotation.relatedto;

import org.springframework.data.neo4j.annotation.IdentifiableEntity;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

@NodeEntity
public class Mondrian extends IdentifiableEntity {
    public static short RED = 1;
    public static short YELLOW = 2;
    public static short BLUE = 3;

    private String title;

    @RelatedTo(type = "includes", enforceTargetType = true)
    private Square square;

    @RelatedTo(type = "includes", enforceTargetType = true)
    private Set<Rectangle> rectangles = new HashSet<Rectangle>();

    @RelatedTo(type = "includes", enforceTargetType = true)
    private Iterable<Quadrilateral> quadrilaterals;

    public Mondrian() {
    }

    public Mondrian(String title) {
        this.title = title;
    }

    public void includes(Square square) {
        this.square = square;
    }

    public void includes(Rectangle... rectangles) {
        this.rectangles.addAll(asList(rectangles));
    }

    public Square getSquare() {
        return square;
    }

    public Set<Rectangle> getRectangles() {
        return rectangles;
    }

    public Iterable<Quadrilateral> getQuadrilaterals() {
        return quadrilaterals;
    }
}
