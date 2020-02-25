package org.neo4j.doc.springframework.data.docs.repositories;

// tag::all[]
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;

@EnableNeo4jRepositories(repositoryImplementationPostfix = "MyPostfix")
public class CustomFragmentPostfix {
}
// end::all[]
