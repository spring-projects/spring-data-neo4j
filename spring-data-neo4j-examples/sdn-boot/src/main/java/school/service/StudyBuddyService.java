package school.service;

import school.domain.StudyBuddy;

import java.util.Map;

public interface StudyBuddyService extends Service<StudyBuddy> {

    Iterable<Map<String,Object>> getStudyBuddiesByPopularity();

}
