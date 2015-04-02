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

package org.neo4j.ogm.unit.typeconversion;

import org.junit.Test;
import org.neo4j.ogm.domain.convertible.numbers.Account;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.typeconversion.AttributeConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class TestNumberConversion {

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.convertible.numbers");
    private static final ClassInfo accountInfo = metaData.classInfo("Account");

    @Test
    public void assertAccountFieldsHaveDefaultConverters() {
        assertTrue(accountInfo.propertyField("balance").hasConverter());
        assertTrue(accountInfo.propertyField("facility").hasConverter());
        assertTrue(accountInfo.propertyField("deposits").hasConverter());
        assertTrue(accountInfo.propertyField("loans").hasConverter());

    }

    @Test
    public void assertAccountMethodsHaveDefaultConverters() {
        assertTrue(accountInfo.propertyGetter("balance").hasConverter());
        assertTrue(accountInfo.propertySetter("balance").hasConverter());

        assertTrue(accountInfo.propertyGetter("facility").hasConverter());
        assertTrue(accountInfo.propertySetter("facility").hasConverter());

        assertTrue(accountInfo.propertyGetter("deposits").hasConverter());
        assertTrue(accountInfo.propertySetter("deposits").hasConverter());

        assertTrue(accountInfo.propertyGetter("loans").hasConverter());
        assertTrue(accountInfo.propertySetter("loans").hasConverter());
    }

    @Test
    public void assertAccountBalanceConverterWorks() {

        AttributeConverter converter = accountInfo.propertyGetter("balance").converter();

        Account account = new Account(new BigDecimal("12345.67"), new BigInteger("1000"));
        assertEquals("12345.67", converter.toGraphProperty(account.getBalance()));

        account.setBalance((BigDecimal) converter.toEntityAttribute("34567.89"));
        assertEquals(new BigDecimal("34567.89"), account.getBalance());
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertAccountDepositConverterWorks() {
        AttributeConverter converter = accountInfo.propertyGetter("deposits").converter();
        BigDecimal[] deposits = new BigDecimal[] {new BigDecimal("12345.67"),new BigDecimal("34567.89")};
        Account account = new Account(new BigDecimal("12345.67"), new BigInteger("1000"));
        account.setDeposits(deposits);
        String[] convertedDeposits = (String[])converter.toGraphProperty(account.getDeposits());
        assertEquals(2, convertedDeposits.length);
        assertEquals("12345.67",convertedDeposits[0]);
        assertEquals("34567.89",convertedDeposits[1]);

        account.setDeposits((BigDecimal[]) converter.toEntityAttribute(convertedDeposits));
        assertEquals(new BigDecimal("12345.67"), account.getDeposits()[0]);
        assertEquals(new BigDecimal("34567.89"), account.getDeposits()[1]);
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertAccountLoNAConverterWorks() {
        AttributeConverter converter = accountInfo.propertyGetter("loans").converter();
        List<BigInteger> loans = new ArrayList<>();
        loans.add(BigInteger.valueOf(123456));
        loans.add(BigInteger.valueOf(567890));
        Account account = new Account(new BigDecimal("12345.67"), new BigInteger("1000"));
        account.setLoans(loans);
        String[] convertedLoans = (String[])converter.toGraphProperty(account.getLoans());
        assertEquals(2, convertedLoans.length);
        assertEquals("123456",convertedLoans[0]);
        assertEquals("567890", convertedLoans[1]);

        account.setLoans((List) converter.toEntityAttribute(convertedLoans));
        assertEquals(BigInteger.valueOf(123456), account.getLoans().get(0));
        assertEquals(BigInteger.valueOf(567890), account.getLoans().get(1));
    }

    @Test
    public void assertAccountFacilityConverterWorks() {

        AttributeConverter converter = accountInfo.propertyGetter("facility").converter();

        Account account = new Account(new BigDecimal("12345.67"), new BigInteger("1000"));
        assertEquals("1000", converter.toGraphProperty(account.getFacility()));

        account.setFacility((BigInteger) converter.toEntityAttribute("2000"));
        assertEquals(new BigInteger("2000"), account.getFacility());
    }

    @Test
    public void assertConvertingNullGraphPropertyWorksCorrectly() {
        AttributeConverter converter = accountInfo.propertyGetter("facility").converter();
        assertEquals(null, converter.toEntityAttribute(null));
        converter = accountInfo.propertyGetter("deposits").converter();
        assertEquals(null, converter.toEntityAttribute(null));
        converter = accountInfo.propertyGetter("loans").converter();
        assertEquals(null, converter.toEntityAttribute(null));
    }

    @Test
    public void assertConvertingNullAttributeWorksCorrectly() {
        AttributeConverter converter = accountInfo.propertyGetter("facility").converter();
        assertEquals(null, converter.toGraphProperty(null));
        converter = accountInfo.propertyGetter("deposits").converter();
        assertEquals(null, converter.toGraphProperty(null));
        converter = accountInfo.propertyGetter("loans").converter();
        assertEquals(null, converter.toGraphProperty(null));
    }
}
