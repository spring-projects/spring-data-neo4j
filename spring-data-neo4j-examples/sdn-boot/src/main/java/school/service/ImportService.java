package school.service;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import school.domain.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class ImportService {

    @Autowired
    private Session session;

    public void reload() {
        session.purgeDatabase();
        session.execute(load("school.cql"));
        //session.save(createNewSchool());
    }

    protected static String load(String cqlFile) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(cqlFile)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(" ");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private School createNewSchool() {

        School school = new School("Hills Road Technical College");

        Map<String, Department> departments = createDepartments("Mathematics", "Science", "Engineering");
        Map<String, Teacher> teachers = createTeachers("Ms Packard-Bell", "Mr Smith", "Mrs Adenough",
                "Mr Van der Graaf", "Mr Balls", "Ms Noethe", "Mrs Noakes", "Mr Marker", "Ms Delgado",
                "Mrs Glass", "Mr Flint", "Mr Kearney", "Mrs Forrester", "Mrs Fischer", "Mr Jameson");
        Map<String, Subject> subjects = createSubjects("Physics", "Chemistry", "Biology", "Earth Science",
                "Pure Mathematics", "Applied Mathematics", "Mechanical Engineering", "Chemical Engineering",
                "Systems Engineering", "Civil Engineering", "Electrical Engineering");

        Map<String, Student> students = createStudents(
                "Portia Vasquez",
                "Andrew Parks",
                "Germane Frye",
                "Yuli Gutierrez",
                "Kamal Solomon",
                "Lysandra Porter",
                "Stella Santiago",
                "Brenda Torres",
                "Heidi Dunlap",
                "Halee Taylor" ,
                "Brennan Crosby" ,
                "Rooney Cook" ,
                "Xavier Morrison" ,
                "Zelenia Santana" ,
                "Eaton Bonner" ,
                "Leilani Bishop" ,
                "Jamalia Pickett" ,
                "Wynter Russell" ,
                "Liberty Melton" ,
                "MacKensie Obrien" ,
                "Oprah Maynard" ,
                "Lyle Parks" ,
                "Madonna Justice" ,
                "Herman Frederick" ,
                "Preston Stevenson" ,
                "Drew Carrillo" ,
                "Hamilton Woodward" ,
                "Buckminster Bradley" ,
                "Shea Cote" ,
                "Raymond Leonard" ,
                "Gavin Branch" ,
                "Kylan Powers" ,
                "Hedy Bowers" ,
                "Derek Church" ,
                "Silas Santiago" ,
                "Elton Bright" ,
                "Dora Schmidt" ,
                "Julian Sullivan" ,
                "Willow Morton" ,
                "Blaze Hines" ,
                "Felicia Tillman" ,
                "Ralph Webb" ,
                "Roth Gilmore" ,
                "Dorothy Burgess" ,
                "Lana Sandoval" ,
                "Nevada Strickland" ,
                "Lucian Franco" ,
                "Jasper Talley" ,
                "Madaline Spears" ,
                "Upton Browning" ,
                "Cooper Leon" ,
                "Celeste Ortega" ,
                "Willa Hewitt" ,
                "Rooney Bryan" ,
                "Nayda Hays" ,
                "Kadeem Salazar" ,
                "Halee Allen" ,
                "Odysseus Mayo" ,
                "Kato Merrill" ,
                "Halee Juarez" ,
                "Chloe Charles" ,
                "Abel Montoya" ,
                "Hilda Welch" ,
                "Britanni Bean" ,
                "Joelle Beach" ,
                "Ciara Odom" ,
                "Zia Williams" ,
                "Darrel Bailey" ,
                "Lance Mcdowell" ,
                "Clayton Bullock" ,
                "Roanna Mosley" ,
                "Amethyst Mcclure" ,
                "Hanae Mann" ,
                "Graiden Haynes" ,
                "Marcia Byrd" ,
                "Yoshi Joyce" ,
                "Gregory Sexton" ,
                "Nash Carey" ,
                "Rae Stevens" ,
                "Blossom Fulton" ,
                "Lev Curry" ,
                "Margaret Gamble" ,
                "Rylee Patterson" ,
                "Harper Perkins" ,
                "Kennan Murphy" ,
                "Hilda Coffey" ,
                "Marah Reed" ,
                "Blaine Wade" ,
                "Geraldine Sanders" ,
                "Kerry Rollins" ,
                "Virginia Sweet" ,
                "Sophia Merrill" ,
                "Hedda Carson" ,
                "Tamekah Charles" ,
                "Knox Barton" ,
                "Ariel Porter" ,
                "Berk Wooten" ,
                "Galena Glenn" ,
                "Jolene Anderson" ,
                "Leonard Hewitt",
                "Maris Salazar" ,
                "Brian Frost" ,
                "Zane Moses" ,
                "Serina Finch" ,
                "Anastasia Fletcher" ,
                "Glenna Chapman" ,
                "Mufutau Gillespie" ,
                "Basil Guthrie" ,
                "Theodore Marsh" ,
                "Jaime Contreras" ,
                "Irma Poole" ,
                "Buckminster Bender" ,
                "Elton Morris" ,
                "Barbara Nguyen" ,
                "Tanya Kidd" ,
                "Kaden Hoover" ,
                "Christopher Bean" ,
                "Trevor Daugherty" ,
                "Rudyard Bates" ,
                "Stacy Monroe" ,
                "Kieran Keller" ,
                "Ivy Garrison" ,
                "Miranda Haynes" ,
                "Abigail Heath" ,
                "Margaret Santiago" ,
                "Cade Floyd" ,
                "Allen Crane" ,
                "Stella Gilliam" ,
                "Rashad Miller" ,
                "Francis Cox" ,
                "Darryl Rosario" ,
                "Michael Daniels" ,
                "Aretha Henderson" ,
                "Roth Barrera" ,
                "Yael Day" ,
                "Wynter Richmond" ,
                "Quyn Flowers" ,
                "Yvette Marquez" ,
                "Teagan Curry" ,
                "Brenden Bishop" ,
                "Montana Black" ,
                "Hayes House" ,
                "Ramona Parker" ,
                "Merritt Hansen" ,
                "Melvin Vang" ,
                "Samantha Perez" ,
                "Thane Porter" ,
                "Vaughan Haynes" ,
                "Irma Miles" ,
                "Amery Jensen" ,
                "Montana Holman" ,
                "Kimberly Langley" ,
                "Ebony Bray" ,
                "Ishmael Pollard" ,
                "Illana Thompson" ,
                "Rhona Bowers" ,
                "Lilah Dotson" ,
                "Shelly Roach" ,
                "Celeste Woodward" ,
                "Christen Lynn" ,
                "Miranda Slater" ,
                "Lunea Clements" ,
                "Lester Francis" ,
                "David Fischer" ,
                "Kyra Bean" ,
                "Imelda Alston" ,
                "Finn Farrell" ,
                "Kirby House" ,
                "Amanda Zamora" ,
                "Rina Franco" ,
                "Sonia Lane" ,
                "Nora Jefferson" ,
                "Colton Ortiz" ,
                "Alden Munoz" ,
                "Ferdinand Cline" ,
                "Cynthia Prince" ,
                "Asher Hurst" ,
                "MacKensie Stevenson" ,
                "Sydnee Sosa" ,
                "Dante Callahan" ,
                "Isabella Santana" ,
                "Raven Bowman" ,
                "Kirby Bolton" ,
                "Peter Shaffer" ,
                "Fletcher Beard" ,
                "Irene Lowe" ,
                "Ella Talley" ,
                "Jorden Kerr" ,
                "Macey Delgado" ,
                "Ulysses Graves" ,
                "Declan Blake" ,
                "Lila Hurst" ,
                "David Rasmussen" ,
                "Desiree Cortez" ,
                "Myles Horton" ,
                "Rylee Willis" ,
                "Kelsey Yates" ,
                "Alika Stanton" ,
                "Ria Campos" ,
                "Elijah Hendricks");

        // department -[:curriculum]-> subjects[]
        assign(departments.get("Science").getSubjects(), subjects, "Physics", "Chemistry", "Biology", "Earth Science");
        assign(departments.get("Mathematics").getSubjects(), subjects, "Pure Mathematics", "Applied Mathematics");
        assign(departments.get("Engineering").getSubjects(), subjects, "Mechanical Engineering", "Chemical Engineering",
                "Systems Engineering", "Civil Engineering", "Electrical Engineering");

        //subject[] <-[:teaches]- teacher
        assign(subjects.get("Physics").getTeachers(), teachers, "Mr Balls", "Mr Kearney");
        assign(subjects.get("Chemistry").getTeachers(), teachers, "Mr Kearney", "Mrs Noakes");
        assign(subjects.get("Biology").getTeachers(), teachers, "Mrs Noakes", "Mrs Fischer");
        assign(subjects.get("Earth Science").getTeachers(), teachers, "Ms Noethe");

        assign(subjects.get("Pure Mathematics").getTeachers(), teachers, "Mr Flint", "Mr Marker", "Mr Van der Graaf");
        assign(subjects.get("Applied Mathematics").getTeachers(), teachers, "Mrs Glass", "Mr Van der Graaf",
                "Ms Packard-Bell");

        assign(subjects.get("Mechanical Engineering").getTeachers(), teachers, "Mr Jameson");
        assign(subjects.get("Chemical Engineering").getTeachers(), teachers, "Mrs Adenough");
        assign(subjects.get("Systems Engineering").getTeachers(), teachers, "Mr Smith");
        assign(subjects.get("Civil Engineering").getTeachers(), teachers, "Ms Delgado");
        assign(subjects.get("Electrical Engineering").getTeachers(), teachers, "Mrs Forrester");

        // department -[:member]-> teachers[]
        assign(departments.get("Science").getTeachers(), teachers,
                "Mr Balls", "Mr Kearney", "Mrs Noakes", "Mrs Fischer", "Ms Noethe");

        assign(departments.get("Mathematics").getTeachers(), teachers,
                "Mr Flint", "Mr Marker", "Mr Van der Graaf", "Mrs Glass", "Ms Packard-Bell");

        assign(departments.get("Engineering").getTeachers(), teachers,
                "Mr Jameson", "Mrs Adenough", "Mr Smith", "Ms Delgado", "Mrs Forrester");


        // school -[:departments]-> departments[]
        school.getDepartments().addAll(departments.values());

        // school -[:staff]-> teachers[]
        school.getTeachers().addAll(teachers.values());

        school.getStudents().addAll(students.values());
        return school;
    }

    private Map<String, Teacher> createTeachers(String... names) {
        Map<String, Teacher> map = new HashMap<>();
        for (String name : names) {
            map.put(name, new Teacher(name));
        }
        return map;
    }

    private Map<String, Student> createStudents(String... names) {
        Map<String, Student> map = new HashMap<>();
        for (String name : names) {
            map.put(name, new Student(name));
        }
        return map;
    }

    private Map<String, Department> createDepartments(String... names) {
        Map<String, Department> map = new HashMap<>();
        for (String name : names) {
            map.put(name, new Department(name));
        }
        return map;
    }

    private Map<String, Subject> createSubjects(String... names) {
        Map<String, Subject> map = new HashMap<>();
        for (String name : names) {
            map.put(name, new Subject(name));
        }
        return map;
    }

    private void assign(Set to, Map from, String... names) {
        for (String name : names) {
            to.add(from.get(name));
        }
    }

}
