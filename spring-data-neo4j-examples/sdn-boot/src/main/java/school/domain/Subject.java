package school.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

public class Subject extends Entity {

    private String name;

    @Relationship(type="CURRICULUM", direction = Relationship.INCOMING)
    private Department department;

    @Relationship(type = "TAUGHT_BY")
    private Set<Teacher> teachers;

    @Relationship(type = "SUBJECT_TAUGHT")
    private Set<Course> courses;

    public Subject(String name) {
        this();
        this.name = name;
    }

    public Subject() {
        this.teachers = new HashSet<>();
        this.courses = new HashSet<>();
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(Set<Teacher> teachers) {
        this.teachers = teachers;
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", department=" + department +
                ", teachers=" + teachers.size() +
                '}';
    }
}
