package school.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;
import school.domain.StudyBuddy;

import java.util.Map;

@Repository
public interface StudyBuddyRepository extends GraphRepository<StudyBuddy> {

    @Query("MATCH(s:StudyBuddy)<-[:BUDDY]-(p:Student) return p, count(s) as buddies ORDER BY buddies DESC")
    Iterable<Map<String, Object>> getStudyBuddiesByPopularity();
}
