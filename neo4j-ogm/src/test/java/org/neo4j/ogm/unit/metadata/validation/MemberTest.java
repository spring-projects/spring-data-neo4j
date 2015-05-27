package org.neo4j.ogm.unit.metadata.validation;

import org.junit.Test;
import org.neo4j.ogm.metadata.info.validation.Member;

import java.util.Collection;
import java.util.Collections;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class MemberTest {

    private static final String LONG = "Ljava/lang/Long;";
    private static final String STRING = "Ljava/lang/String;";

    @Test
    public void shouldReturnTrueWhenTypeIsLong() {
        Member member = new Member("id", LONG, Collections.EMPTY_LIST);

        boolean isType = member.isType(Long.class);

        assertTrue(isType);
    }

    @Test
    public void shouldReturnFalseWhenTypeIsString() {
        Member member = new Member("id", STRING, Collections.EMPTY_LIST);

        boolean isType = member.isType(Long.class);

        assertFalse(isType);
    }

    @Test
    public void shouldTranslateNamesToMembers() {
        String[] names = new String[]{"User", "Profile", "Account"};
        Member userMember = new Member("User");
        Member profileMember = new Member("Profile");
        Member accountMember = new Member("Account");

        Collection<Member> members = Member.translateNamesToMembers(names);

        assertEquals(3, members.size());
        assertTrue(members.contains(userMember));
        assertTrue(members.contains(profileMember));
        assertTrue(members.contains(accountMember));
    }
}
