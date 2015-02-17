package school.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

public class School extends Entity {

    private String name;

    @Relationship(type = "DEPARTMENT")
    private Set<Department> departments;

    @Relationship(type = "STAFF")
    private Set<Teacher> teachers;

    @Relationship(type = "HEAD_TEACHER")
    private Teacher headTeacher;

    @Relationship(type = "STUDENT")
    private Set<Student> students;

    public School() {
        this.departments = new HashSet<>();
        this.teachers = new HashSet<>();
        this.students = new HashSet<>();
    }

    public School(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(Set<Department> departments) {
        this.departments = departments;
    }

    public Teacher getHeadTeacher() {
        return headTeacher;
    }

    public void setHeadTeacher(Teacher headTeacher) {
        this.headTeacher = headTeacher;
    }

    public Set<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(Set<Teacher> teachers) {
        this.teachers = teachers;
    }

    public Set<Student> getStudents() {
        return students;
    }

    public void setStudents(Set<Student> students) {
        this.students = students;
    }

    @Override
    public String toString() {
        return "School{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", departments=" + departments.size() +
                ", teachers=" + teachers.size() +
                ", students=" + students.size() +
                '}';
    }
}

