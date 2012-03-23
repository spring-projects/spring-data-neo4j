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
package org.springframework.data.neo4j.inheritance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.inheritance.model.Project;
import org.springframework.data.neo4j.inheritance.model.ProjectDetail;
import org.springframework.data.neo4j.inheritance.model.ProjectDetailRelationship;
import org.springframework.data.neo4j.inheritance.repository.ProjectDetailRepository;
import org.springframework.data.neo4j.inheritance.repository.ProjectRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.IteratorUtil.count;

/**
 * @author mh
 * @since 23.03.12
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InheritanceTest {
    private String validName="projectDetail";

    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectDetailRepository projectDetailRepository;
    @Autowired Neo4jTemplate template;
    @Test
    @Transactional
    public void testCreatedProjectWithInheritance() throws Exception {
        Project project = new Project();
        project = projectRepository.save(project);

        ProjectDetail projectDetail = new ProjectDetail(validName);
        projectDetailRepository.save(projectDetail);

        ProjectDetailRelationship projectDetailRelationship = new ProjectDetailRelationship(null, project, projectDetail);
        template.save(projectDetailRelationship);

        project.getProjectDetailRelationships().add(projectDetailRelationship);
        project = projectRepository.save(project);

        assertEquals(1, count(projectRepository.findAll()));
        assertEquals(1, count(projectDetailRepository.findAll()));

        ProjectDetail actualProjectDetail = projectDetailRepository.findAll().iterator().next();
        assertEquals(validName, actualProjectDetail.getName());
        assertEquals(project.getEntityId(), actualProjectDetail.getParentProject().getEntityId());

        //all tests above pass without issue -- the test below fails: expected:<1> but was:<0>
        Project actualProject = projectRepository.findOne(project.getEntityId());
        assertEquals(1, actualProject.getProjectDetailRelationships().size());
    }
}
