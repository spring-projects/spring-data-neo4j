package school.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;


public class Student extends Entity {

    private String name;

    @Relationship(type = "ENROLLED")
    private Set<Course> courses;

    @Relationship(type="BUDDY", direction=Relationship.INCOMING)
    private Set<StudyBuddy> studyBuddies;

    public Student() {
        this.studyBuddies = new HashSet<>();
        this.courses = new HashSet<>();
    }

    public Student(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<StudyBuddy> getStudyBuddies() {
        return studyBuddies;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Course> getCourses() {
        return courses;
    }

    public void setCourses( Set<Course> courses ) {
        this.courses = courses;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", courses=" + courses.size() +
                ", studyBuddies=" + studyBuddies.size() +
                '}';
    }
}
