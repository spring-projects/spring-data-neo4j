/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.nativetypes;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.List;

@NodeEntity
public class TimeDomain {
	@Id
	@GeneratedValue
	private Long id;

	private LocalDate localDate;
	private LocalDateTime localDateTime;
	private Date date;
	private Duration duration;
	private Period period;
	private TemporalAmount temporalAmount;

	private Duration[] arrayOfDurations;

	private List<Duration> listOfDurations;

	public LocalDate getLocalDate() {
		return localDate;
	}

	public void setLocalDate(LocalDate localDate) {
		this.localDate = localDate;
	}

	public LocalDateTime getLocalDateTime() {
		return localDateTime;
	}

	public void setLocalDateTime(LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public Period getPeriod() {
		return period;
	}

	public void setPeriod(Period period) {
		this.period = period;
	}

	public TemporalAmount getTemporalAmount() {
		return temporalAmount;
	}

	public void setTemporalAmount(TemporalAmount temporalAmount) {
		this.temporalAmount = temporalAmount;
	}

	public Duration[] getArrayOfDurations() {
		return arrayOfDurations;
	}

	public void setArrayOfDurations(Duration[] arrayOfDurations) {
		this.arrayOfDurations = arrayOfDurations;
	}

	public List<Duration> getListOfDurations() {
		return listOfDurations;
	}

	public void setListOfDurations(List<Duration> listOfDurations) {
		this.listOfDurations = listOfDurations;
	}
}
