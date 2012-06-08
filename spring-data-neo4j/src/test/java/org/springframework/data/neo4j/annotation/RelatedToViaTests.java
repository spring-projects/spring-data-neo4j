/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.annotation;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-via-test-context.xml"})
@Transactional
public class RelatedToViaTests
{
    public static final String DISTRICT_LINE = "District Line";
    public static final String CENTRAL_LINE = "Central Line";

    @Autowired
    private UndergroundRepository tfl;

    @Before
    public void before()
    {
        tfl.deleteAll();
    }

    @Test
    public void shouldMapRelationshipAsFirstClassCitizen() throws Exception
    {
        TubeStation westHam = tfl.save( new TubeStation( "West Ham" ) );
        TubeStation stratford = tfl.save( new TubeStation( "Stratford" ) );
        TubeStation mileEnd = new TubeStation( "Mile End" );
        mileEnd.connectsTo( westHam, DISTRICT_LINE );
        mileEnd.connectsTo( stratford, CENTRAL_LINE );

        tfl.save( mileEnd );

        mileEnd = tfl.findOne( mileEnd.getId() );
        Line line = mileEnd.getLines().iterator().next();
        assertThat( mileEnd.getId(), is( equalTo( line.getOrigin().getId() ) ) );
        assertThat( asList( DISTRICT_LINE, CENTRAL_LINE ), hasItem( line.getName() ) );
        assertThat( asList( westHam.getId(), stratford.getId() ), hasItem( line.getDestination().getId() ) );
    }

    @Test
    public void shouldValidateEndNode() throws Exception
    {
        TubeStation mileEnd = new TubeStation( "East Ham" );
        mileEnd.connectsTo( null, DISTRICT_LINE );

        try
        {
            tfl.save( mileEnd );

            fail();
        }
        catch ( InvalidDataAccessApiUsageException e )
        {
            assertThat( e.getCause().getMessage(), is( equalTo( "End node must not be null (org.springframework.data" +
                    ".neo4j.annotation.Line)" ) ) );
        }
    }
}
