package org.neo4j.ogm.unit.metadata.validation;

import org.junit.Test;
import org.neo4j.ogm.metadata.info.validation.Member;
import org.neo4j.ogm.metadata.info.validation.Members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class MembersTest {

    private static final String LONG = "Ljava/lang/Long;";
    private static final String STRING = "Ljava/lang/String;";

    @Test
    public void shouldReturnTrueForOneRequiredMember() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Required();

        assertTrue(valid);
    }

    @Test
    public void shouldReturnTrueForTwoRequiredMember() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Profile", STRING, Collections.EMPTY_LIST));
            add(new Member("Account", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
            add(new Member("Profile"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Required();

        assertTrue(valid);
    }

    @Test
    public void shouldReturnFalseForOneMissingMember() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("Profile"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Required();

        assertFalse(valid);
    }

    @Test
    public void shouldReturnFalseForTwoMissingMember() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Profile", STRING, Collections.EMPTY_LIST));
            add(new Member("Account", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("Currency"));
            add(new Member("Country"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Required();

        assertFalse(valid);
    }

    @Test
    public void shouldReturnFalseForOneMissingMemberFromTwo() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Profile", STRING, Collections.EMPTY_LIST));
            add(new Member("Account", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("Profile"));
            add(new Member("Currency"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Required();

        assertFalse(valid);
    }

    @Test
    public void shouldReturnFalseForOneForbidden() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Profile", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Forbidden();

        assertFalse(valid);
    }

    @Test
    public void shouldReturnFalseForTwoForbidden() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Profile", STRING, Collections.EMPTY_LIST));
            add(new Member("Account", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
            add(new Member("Profile"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Forbidden();

        assertFalse(valid);
    }

    @Test
    public void shouldReturnTrueForOneForbiddenFromTwo() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Profile", STRING, Collections.EMPTY_LIST));
            add(new Member("Account", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
            add(new Member("Country"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Forbidden();

        assertTrue(valid);
    }

    @Test
    public void shouldReturnTrueForWhenTwoForbiddenAreMissing() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Profile", STRING, Collections.EMPTY_LIST));
            add(new Member("Account", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("Currency"));
            add(new Member("Country"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Forbidden();

        assertTrue(valid);
    }

    @Test
    public void shouldReturnTrueForUnique() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Unique();

        assertTrue(valid);
    }

    @Test
    public void shouldReturnFalseForUniqueIsNotPresent() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("Country"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Unique();

        assertFalse(valid);
    }

    @Test
    public void shoudlReturnFalseForUniqueIsPresentMultiple() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
            add(new Member("Account", STRING, Collections.EMPTY_LIST));
            add(new Member("Country", STRING, Collections.EMPTY_LIST));
            add(new Member("Country", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("Country"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Unique();

        assertFalse(valid);
    }

    @Test
    public void shouldReturnTrueForTypeCheck() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", LONG, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Type(Long.class);

        assertTrue(valid);
    }

    @Test
    public void shouldReturnFalseForTypeCheck() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("Profile"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Type(Long.class);

        assertFalse(valid);
    }

    @Test
    public void shouldReturnAnnotations() {
        Collection<Member> current = new ArrayList<Member>(){{
            add(new Member("User", STRING, Collections.EMPTY_LIST));
        }};

        Collection<Member> required = new ArrayList<Member>(){{
            add(new Member("User"));
        }};

        Members members = new Members(current, required);

        boolean valid = members.Required();

        assertTrue(valid);
    }
}
