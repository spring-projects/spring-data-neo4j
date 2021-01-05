/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.conversion;

import java.lang.annotation.ElementType;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Result;
import org.neo4j.harness.ServerControls;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.integration.conversion.domain.JavaElement;
import org.springframework.data.neo4j.integration.conversion.domain.MonetaryAmount;
import org.springframework.data.neo4j.integration.conversion.domain.PensionPlan;
import org.springframework.data.neo4j.integration.conversion.domain.SiteMember;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See DATAGRAPH-624
 *
 * @author Adam George
 * @author Luanne Misquitta
 * @author Vince Bickers
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConversionServiceTests.ConversionServicePersistenceContext.class)
public class ConversionServiceTests {

	@Autowired private ServerControls neo4jTestServer;
	@Autowired private PensionRepository pensionRepository;
	@Autowired private JavaElementRepository javaElementRepository;
	@Autowired private SiteMemberRepository siteMemberRepository;
	// TODO See below, for the time being at least be explicit on which type of conversion service we're working on here
	// The only thing that is under test, is the instance of MetaDataDrivenConversionService which get's even
	// modified heavily be this test. This needs to be fixed in the near future.
	// Also it doesn't test conversion with `graphPropertyType` attribute and a standard Spring Converter.
	@Autowired private MetaDataDrivenConversionService conversionService;

	@Autowired Session session;

	@Autowired private TransactionTemplate transactionTemplate;

	@Before
	public void setUp() {
		neo4jTestServer.graph().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	/**
	 * This should work by virtue of the fact that there's an OGM-level converter defined on a class we've scanned.
	 */
	@Test
	public void shouldBeAbleToConvertBetweenBytesAndBase64EncodedDataViaSpringConversionService() {
		String base64Representation = "YmNkZWY=";
		byte[] binaryData = new byte[] { 98, 99, 100, 101, 102 };

		assertThat(this.conversionService.canConvert(byte[].class, String.class))
				.isTrue();
		assertThat(this.conversionService.convert(binaryData, String.class))
				.isEqualTo(base64Representation);

		assertThat(this.conversionService.canConvert(String.class, byte[].class))
				.isTrue();
		assertThat(Arrays.equals(binaryData, this.conversionService
				.convert(base64Representation, byte[].class))).isTrue();
	}

	@Test
	public void shouldConvertBase64StringOutOfGraphDatabaseBackIntoByteArray() {
		Result rs = neo4jTestServer.graph()
				.execute("CREATE (u:SiteMember {profilePictureData:'MTIzNDU2Nzg5'}) RETURN id(u) AS userId");
		Long userId = (Long) rs.columnAs("userId").next();

		byte[] expectedData = "123456789".getBytes();

		SiteMember siteMember = this.siteMemberRepository.findById(userId).get();
		assertThat(Arrays.equals(expectedData, siteMember.getProfilePictureData()))
				.as("The data wasn't converted correctly").isTrue();
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

		Result result = neo4jTestServer.graph().execute("MATCH (p:PensionPlan) RETURN p.fundValue AS fv");
		assertThat(result.hasNext()).as("Nothing was saved").isTrue();
		assertThat(String.valueOf(result.next().get("fv")))
				.as("The amount wasn't converted and persisted correctly")
				.isEqualTo("1647281");
		result.close();

		PensionPlan reloadedPension = this.pensionRepository.findById(pensionToSave.getPensionPlanId()).get();
		assertThat(reloadedPension.getFundValue())
				.as("The amount was converted incorrectly")
				.isEqualTo(pensionToSave.getFundValue());
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
		Result result = neo4jTestServer.graph().execute("MATCH (p:PensionPlan) RETURN p.fundValue AS fv");
		assertThat(result.hasNext()).as("Nothing was saved").isTrue();
		assertThat(String.valueOf(result.next().get("fv")))
				.as("The amount wasn't converted and persisted correctly")
				.isEqualTo("2000000");
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

		Result result = neo4jTestServer.graph().execute("MATCH (e:JavaElement) RETURN e.elementType AS type");
		assertThat(result.hasNext()).as("Nothing was saved").isTrue();
		assertThat(result.next().get("type"))
				.as("The element type wasn't converted and persisted correctly")
				.isEqualTo("METHOD");
		result.close();

		JavaElement loadedObject = this.javaElementRepository.findAll().iterator().next();
		assertThat(loadedObject.getElementType())
				.as("The element type wasn't loaded and converted correctly")
				.isEqualTo(ElementType.METHOD);
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

		assertThat(this.conversionService.canConvert(byte[].class, String.class))
				.isTrue();
		assertThat(this.conversionService.convert(binaryData, String.class))
				.isEqualTo(base64Representation);

		assertThat(this.conversionService.canConvert(String.class, byte[].class))
				.isTrue();
		assertThat(Arrays.equals(binaryData, this.conversionService
				.convert(base64Representation, byte[].class))).isTrue();

		SiteMember siteMember = new SiteMember();
		siteMember.setProfilePictureData(binaryData);
		this.siteMemberRepository.save(siteMember);

		siteMember = session.loadAll(SiteMember.class).iterator().next();
		assertThat(siteMember.getProfilePictureData()).isEqualTo(binaryData);
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
		assertThat(siteMember.getProfilePictureData()).isEqualTo(binaryData);
		assertThat(siteMember.getYears().intValue()).isEqualTo(50);
	}

	@Test // DATAGRAPH-659
	public void shouldRecognizeJavaEnums() {
		SiteMember siteMember = new SiteMember();
		siteMember.setRoundingModes(Arrays.asList(RoundingMode.DOWN, RoundingMode.FLOOR));
		this.siteMemberRepository.save(siteMember);

		siteMember = session.loadAll(SiteMember.class).iterator().next();
		assertThat(siteMember.getRoundingModes().size()).isEqualTo(2);
		assertThat(siteMember.getRoundingModes().contains(RoundingMode.DOWN)).isTrue();
		assertThat(siteMember.getRoundingModes().contains(RoundingMode.FLOOR)).isTrue();
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.integration.conversion.domain",
			repositoryPackages = "org.springframework.data.neo4j.integration.conversion")
	static class ConversionServicePersistenceContext {

		@Bean
		public ConversionService conversionService(SessionFactory sessionFactory) {
			return new MetaDataDrivenConversionService(sessionFactory.metaData());
		}
	}
}
