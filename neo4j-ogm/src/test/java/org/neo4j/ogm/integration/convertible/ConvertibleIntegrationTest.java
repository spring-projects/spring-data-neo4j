/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.neo4j.ogm.integration.convertible;

import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.annotation.typeconversion.DateString;
import org.neo4j.ogm.domain.convertible.date.Memo;
import org.neo4j.ogm.domain.convertible.enums.Education;
import org.neo4j.ogm.domain.convertible.enums.Gender;
import org.neo4j.ogm.domain.convertible.enums.Person;
import org.neo4j.ogm.domain.convertible.numbers.Account;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

/**
 * @author Luanne Misquitta
 */
public class ConvertibleIntegrationTest extends InMemoryServerTest {

    @BeforeClass
    public static void init() throws IOException {
        setUp();
        session = new SessionFactory("org.neo4j.ogm.domain.convertible").openSession("http://localhost:" + neoPort);
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void shouldSaveAndRetrieveEnums() {
        List<Education> completed = new ArrayList<>();
        completed.add(Education.HIGHSCHOOL);
        completed.add(Education.BACHELORS);

        Person person = new Person();
        person.setName("luanne");
        person.setInProgressEducation(new Education[]{Education.MASTERS, Education.PHD});
        person.setCompletedEducation(completed);
        person.setGender(Gender.FEMALE);
        session.save(person);

        Person luanne = session.loadByProperty(Person.class,new Property<String, Object>("name","luanne")).iterator().next();
        assertEquals(Gender.FEMALE, luanne.getGender());
        assertTrue(luanne.getCompletedEducation().contains(Education.HIGHSCHOOL));
        assertTrue(luanne.getCompletedEducation().contains(Education.BACHELORS));
        assertEquals(2, luanne.getInProgressEducation().length);
        assertTrue(luanne.getInProgressEducation()[0].equals(Education.MASTERS) || luanne.getInProgressEducation()[1].equals(Education.MASTERS));
        assertTrue(luanne.getInProgressEducation()[0].equals(Education.PHD) || luanne.getInProgressEducation()[1].equals(Education.PHD));
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void shouldSaveAndRetrieveDates() throws ParseException {
        SimpleDateFormat simpleDateISO8601format = new SimpleDateFormat(DateString.ISO_8601);
        simpleDateISO8601format.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar date0 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        date0.setTimeInMillis(0);
        Calendar date20000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        date20000.setTimeInMillis(20000);
        Set<Date> implementations = new HashSet<>();
        implementations.add(date0.getTime());
        implementations.add(date20000.getTime());

        Calendar date40000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        date40000.setTimeInMillis(40000);
        Calendar date100000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        date100000.setTimeInMillis(100000);
        Date[] escalations = new Date[] {date40000.getTime(), date100000.getTime()};

        Calendar actioned = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        actioned.setTimeInMillis(20000);

        Memo memo = new Memo();
        memo.setMemo("theMemo");
        memo.setImplementations(implementations);
        memo.setEscalations(escalations);
        memo.setActioned(actioned.getTime());
        session.save(memo);

        Memo loadedMemo = session.loadByProperty(Memo.class,new Property<String, Object>("memo","theMemo")).iterator().next();

        Calendar loadedCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        loadedCal.setTime(loadedMemo.getActioned());
        assertEquals(actioned.get(Calendar.DAY_OF_MONTH), loadedCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(actioned.get(Calendar.MONTH), loadedCal.get(Calendar.MONTH));
        assertEquals(actioned.get(Calendar.YEAR), loadedCal.get(Calendar.YEAR));

        assertEquals(2, loadedMemo.getImplementations().size());
        Iterator<Date> implementationsIter = loadedMemo.getImplementations().iterator();
        loadedCal.setTime(implementationsIter.next());
        assertEquals(date0.get(Calendar.DAY_OF_MONTH), loadedCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(date0.get(Calendar.MONTH), loadedCal.get(Calendar.MONTH));
        assertEquals(date0.get(Calendar.YEAR), loadedCal.get(Calendar.YEAR));

        loadedCal.setTime(implementationsIter.next());
        assertEquals(date20000.get(Calendar.DAY_OF_MONTH), loadedCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(date20000.get(Calendar.MONTH), loadedCal.get(Calendar.MONTH));
        assertEquals(date20000.get(Calendar.YEAR), loadedCal.get(Calendar.YEAR));

        assertEquals(2, loadedMemo.getEscalations().length);
        loadedCal.setTime(loadedMemo.getEscalations()[0]);
        assertEquals(date40000.get(Calendar.DAY_OF_MONTH), loadedCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(date40000.get(Calendar.MONTH), loadedCal.get(Calendar.MONTH));
        assertEquals(date40000.get(Calendar.YEAR), loadedCal.get(Calendar.YEAR));

        loadedCal.setTime(loadedMemo.getEscalations()[1]);
        assertEquals(date100000.get(Calendar.DAY_OF_MONTH), loadedCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(date100000.get(Calendar.MONTH), loadedCal.get(Calendar.MONTH));
        assertEquals(date100000.get(Calendar.YEAR), loadedCal.get(Calendar.YEAR));

    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void shouldSaveAndRetrieveNumbers() {

        Account account = new Account(new BigDecimal("12345.67"), new BigInteger("1000"));


        BigDecimal[] deposits = new BigDecimal[] {new BigDecimal("12345.67"),new BigDecimal("34567.89")};

        List<BigInteger> loans = new ArrayList<>();
        loans.add(BigInteger.valueOf(123456));
        loans.add(BigInteger.valueOf(567890));

        account.setLoans(loans);
        account.setDeposits(deposits);

        session.save(account);

        assertEquals(new BigDecimal("12345.67"),account.getBalance());


        Account loadedAccount = session.loadAll(Account.class).iterator().next();
        assertEquals(new BigDecimal("12345.67"),loadedAccount.getBalance());
        assertEquals(new BigInteger("1000"),loadedAccount.getFacility());
        assertEquals(loans,loadedAccount.getLoans());
        assertSameArray(deposits,loadedAccount.getDeposits());

    }

    public void assertSameArray(Object[] as, Object[] bs) {

        if (as == null || bs == null) fail("null arrays not allowed");
        if (as.length != bs.length) fail("arrays are not same length");


        for (Object a : as ) {
            boolean found = false;
            for (Object b : bs) {
                if (b.toString().equals(a.toString())) {
                    found = true;
                    break;
                }
            }
            if (!found) fail("array contents are not the same: " + as + ", " + bs);
        }
    }

}
