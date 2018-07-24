/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.integration.conversion;

import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.integration.conversion.domain.JavaElement;
import org.springframework.data.neo4j.integration.conversion.domain.MonetaryAmount;
import org.springframework.data.neo4j.integration.conversion.domain.PensionPlan;
import org.springframework.data.neo4j.integration.conversion.domain.SiteMember;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Adam George
 * @author Luanne Misquitta
 * @author Vince Bickers
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @see DATAGRAPH-624
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ConversionServiceTests.ConversionServicePersistenceContext.class })
public class ConversionServiceTests extends MultiDriverTestClass {

	@Autowired PlatformTransactionManager platformTransactionManager;
	@Autowired private PensionRepository pensionRepository;
	@Autowired private JavaElementRepository javaElementRepository;
	@Autowired private SiteMemberRepository siteMemberRepository;
	@Autowired private GenericConversionService conversionService;

	@Autowired Session session;

	private TransactionTemplate transactionTemplate;

	@Before
	public void setUp() {
		transactionTemplate = new TransactionTemplate(platformTransactionManager);
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	/**
	 * This should work by virtue of the fact that there's an OGM-level converter defined on a class we've scanned.
	 */
	@Test
	public void shouldBeAbleToConvertBetweenBytesAndBase64EncodedDataViaSpringConversionService() {
		String base64Representation = "YmNkZWY=";
		byte[] binaryData = new byte[] { 98, 99, 100, 101, 102 };

		assertTrue(this.conversionService.canConvert(byte[].class, String.class));
		assertEquals(base64Representation, this.conversionService.convert(binaryData, String.class));

		assertTrue(this.conversionService.canConvert(String.class, byte[].class));
		assertTrue(Arrays.equals(binaryData, this.conversionService.convert(base64Representation, byte[].class)));
	}

	@Test
	public void shouldConvertBase64StringOutOfGraphDatabaseBackIntoByteArray() {
		Result rs = getGraphDatabaseService()
				.execute("CREATE (u:SiteMember {profilePictureData:'MTIzNDU2Nzg5'}) RETURN id(u) AS userId");
		Long userId = (Long) rs.columnAs("userId").next();

		byte[] expectedData = "123456789".getBytes();

		SiteMember siteMember = this.siteMemberRepository.findById(userId).get();
		assertTrue("The data wasn't converted correctly", Arrays.equals(expectedData, siteMember.getProfilePictureData()));
	}

	@Test
	public void shouldConvertFieldsUsingSpringConvertersAddedDirectlyToConversionService() {

		PensionPlan pensionToSave = transactionTemplate.execute(status -> {
			conversionService.addConverter(new SpringMonetaryAmountToIntegerConverter());
			conversionService.addConverter(new SpringIntegerToMonetaryAmountConverter());
			conversionService.addConverter(new SpringMonetaryAmountToLongConverter());
			conversionService.addConverter(new SpringLongToMonetaryAmountConverter());

			PensionPlan pensionToSave1 = new PensionPlan(new MonetaryAmount(16472, 81), "Tightfist Asset Management Ltd");

			pensionRepository.save(pensionToSave1);
			return pensionToSave1;
		});

		Result result = getGraphDatabaseService().execute("MATCH (p:PensionPlan) RETURN p.fundValue AS fv");
		assertTrue("Nothing was saved", result.hasNext());
		assertEquals("The amount wasn't converted and persisted correctly", "1647281",
				String.valueOf(result.next().get("fv")));
		result.close();

		PensionPlan reloadedPension = this.pensionRepository.findById(pensionToSave.getPensionPlanId()).get();
		assertEquals("The amount was converted incorrectly", pensionToSave.getFundValue(), reloadedPension.getFundValue());
	}

	/**
	 * If target graph type is set to Integer but we have a converter for a Number then it should still save to the graph.
	 */
	@Test
	public void shouldConvertFieldsUsingAnAvailableSupertypeConverterIfExactTypesDoNotMatch() {

		this.conversionService.addConverterFactory(new SpringMonetaryAmountToNumberConverterFactory());
		// this.conversionService.addConverter(new SpringIntegerToMonetaryAmountConverter());

		PensionPlan pension = new PensionPlan(new MonetaryAmount(20_000, 00), "Ashes Assets LLP");
		this.pensionRepository.save(pension);
		Result result = getGraphDatabaseService().execute("MATCH (p:PensionPlan) RETURN p.fundValue AS fv");
		assertTrue("Nothing was saved", result.hasNext());
		assertEquals("The amount wasn't converted and persisted correctly", "2000000",
				String.valueOf(result.next().get("fv")));
		result.close();
	}

	/**
	 * This should fix DATAGRAPH-659 too.
	 */
	@Test
	public void shouldOperateWithDefaultSpringConvertersToConvertObjectTypesNotInMetaData() {
		DefaultConversionService.addDefaultConverters(this.conversionService);

		JavaElement method = new JavaElement();
		method.setName("toString");
		method.setElementType(ElementType.METHOD);

		this.javaElementRepository.save(method);

		Result result = getGraphDatabaseService().execute("MATCH (e:JavaElement) RETURN e.elementType AS type");
		assertTrue("Nothing was saved", result.hasNext());
		assertEquals("The element type wasn't converted and persisted correctly", "METHOD", result.next().get("type"));
		result.close();

		JavaElement loadedObject = this.javaElementRepository.findAll().iterator().next();
		assertEquals("The element type wasn't loaded and converted correctly", ElementType.METHOD,
				loadedObject.getElementType());
	}

	@Test(expected = ConverterNotFoundException.class)
	public void shouldThrowExceptionIfSuitableConverterIsNotFound() {
		this.conversionService.addConverterFactory(new SpringMonetaryAmountToNumberConverterFactory());

		PensionPlan pension = new PensionPlan(new MonetaryAmount(20_000, 00), "Ashes Assets LLP");
		pension.setJavaElement(new JavaElement());
		this.pensionRepository.save(pension);
	}

	@Test
	public void shouldUseSpecifiedAttributeConverterInsteadOfSprings() {
		// We're registering Spring converters as well
		this.conversionService.addConverter(new SpringIntegerToByteArrayConverter());
		this.conversionService.addConverter(new SpringByteArrayToIntegerConverter());

		String base64Representation = "YmNkZWY=";
		byte[] binaryData = new byte[] { 98, 99, 100, 101, 102 };

		assertTrue(this.conversionService.canConvert(byte[].class, String.class));
		assertEquals(base64Representation, this.conversionService.convert(binaryData, String.class));

		assertTrue(this.conversionService.canConvert(String.class, byte[].class));
		assertTrue(Arrays.equals(binaryData, this.conversionService.convert(base64Representation, byte[].class)));

		SiteMember siteMember = new SiteMember();
		siteMember.setProfilePictureData(binaryData);
		this.siteMemberRepository.save(siteMember);

		siteMember = session.loadAll(SiteMember.class).iterator().next();
		assertArrayEquals(binaryData, siteMember.getProfilePictureData());
	}

	@Test
	public void shouldUseDefaultAttributeConverterInsteadOfSprings() {
		// We're registering Spring converters which should not override the default ogm BigInteger converter
		this.conversionService.addConverter(new SpringBigIntegerToBooleanConverter());
		this.conversionService.addConverter(new SpringBooleanToBigIntegerConverter());

		byte[] binaryData = new byte[] { 98, 99, 100, 101, 102 };

		SiteMember siteMember = new SiteMember();
		siteMember.setProfilePictureData(binaryData);
		siteMember.setYears(BigInteger.valueOf(50));
		this.siteMemberRepository.save(siteMember);

		siteMember = session.loadAll(SiteMember.class).iterator().next();
		assertArrayEquals(binaryData, siteMember.getProfilePictureData());
		assertEquals(50, siteMember.getYears().intValue());
	}

	/**
	 * @see DATAGRAPH-659
	 */
	@Test
	public void shouldRecognizeJavaEnums() {
		SiteMember siteMember = new SiteMember();
		siteMember.setRoundingModes(Arrays.asList(RoundingMode.DOWN, RoundingMode.FLOOR));
		this.siteMemberRepository.save(siteMember);

		siteMember = session.loadAll(SiteMember.class).iterator().next();
		assertEquals(2, siteMember.getRoundingModes().size());
		assertTrue(siteMember.getRoundingModes().contains(RoundingMode.DOWN));
		assertTrue(siteMember.getRoundingModes().contains(RoundingMode.FLOOR));
	}

	@Configuration
	@EnableNeo4jRepositories(
			basePackageClasses = { SiteMemberRepository.class, PensionRepository.class, JavaElementRepository.class })
	@EnableTransactionManagement
	static class ConversionServicePersistenceContext {

		@Bean
		public ConversionService conversionService() {
			return new MetaDataDrivenConversionService(sessionFactory().metaData());
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.integration.conversion.domain");
		}
	}
}
