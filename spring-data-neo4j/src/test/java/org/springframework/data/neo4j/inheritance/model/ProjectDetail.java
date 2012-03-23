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
package org.springframework.data.neo4j.inheritance.model;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

public class ProjectDetail extends BasicEntity {
    private String name;

    @Fetch
    @RelatedTo(type = ProjectDetailRelationship.TYPE, direction = Direction.INCOMING)
    private Project parentProject;

    public ProjectDetail() {
    }

    public ProjectDetail(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Project getParentProject() {
        return parentProject;
    }
}