#!/usr/bin/env bash

#
# Builds a database of all Spring Boot versions that ship with a starter for Spring Data Neo4j.
# This script requires Maven, xidel and DuckDB on the path.
#

set -euo pipefail
export LC_ALL=en_US.UTF-8

DB="$(pwd)/$1"

# Support matrix from
# https://spring.io/projects/spring-boot#support
duckdb "$DB" \
-s "
CREATE TABLE IF NOT EXISTS support_matrix AS SELECT * FROM VALUES
('3.4', '2024-11-21'::date,	'2025-11-21'::date, '2027-02-21'::date),
('3.3', '2024-05-23'::date,	'2025-05-23'::date, '2026-08-23'::date),
('3.2', '2023-11-23'::date,	'2024-11-23'::date, '2026-02-23'::date),
('3.1', '2023-05-18'::date,	'2024-05-18'::date, '2025-08-18'::date),
('3.0', '2022-11-24'::date,	'2023-11-24'::date, '2025-02-24'::date),
('2.7', '2022-05-19'::date,	'2023-11-24'::date, '2026-12-31'::date),
('2.6', '2021-11-17'::date,	'2022-11-24'::date, '2024-02-24'::date),
('2.5', '2021-05-20'::date,	'2022-05-19'::date, '2023-08-24'::date),
('2.4', '2020-11-12'::date,	'2021-11-18'::date, '2023-02-23'::date),
('2.3', '2020-05-15'::date,	'2021-05-20'::date, '2022-08-20'::date),
('2.2', '2019-10-16'::date,	'2020-10-16'::date, '2022-01-16'::date),
('2.1', '2018-10-30'::date,	'2019-10-30'::date, '2021-01-30'::date),
('2.0', '2018-03-01'::date,	'2019-03-01'::date, '2020-06-01'::date),
('1.5', '2017-01-30'::date,	'2019-08-06'::date, '2020-11-06'::date)
src(spring_boot, initial_release, end_of_oss_support, end_of_commercial_support)
" \
-s "CREATE SEQUENCE IF NOT EXISTS version_id" \
-s "CREATE TABLE IF NOT EXISTS versions (
       id  INTEGER PRIMARY KEY DEFAULT(nextval('version_id')),
       spring_boot        VARCHAR(32) NOT NULL,
       release_date       DATE,
       spring_data_neo4j  VARCHAR(32),
       neo4j_ogm          VARCHAR(32),
       neo4j_java_driver  VARCHAR(32),
       CONSTRAINT spring_boot_unique UNIQUE(spring_boot)
   )" \
-s "CREATE OR REPLACE FUNCTION f_make_version(string) AS (
      SELECT list_transform(string_split(string, '.'), x -> TRY_CAST (x AS INTEGER))
   )
" \
-s "
CREATE OR REPLACE VIEW v_versions AS (
  SELECT v.* EXCLUDE(id, release_date), release_date, end_of_oss_support, end_of_commercial_support
  FROM versions v
  ASOF LEFT JOIN support_matrix sm ON f_make_version(v.spring_boot) >= f_make_version(sm.spring_boot)
  ORDER BY f_make_version(v.spring_boot) ASC
)" \
-s "
CREATE OR REPLACE VIEW v_oss_supported_versions AS (
  SELECT *
  FROM v_versions v
  WHERE end_of_oss_support >= today()
  ORDER BY f_make_version(v.spring_boot) ASC
)"  \
 -s "
CREATE OR REPLACE VIEW v_commercially_supported_versions AS (
   SELECT *
   FROM v_versions v
   WHERE end_of_commercial_support >= today()
   ORDER BY f_make_version(v.spring_boot) ASC
 )"

mkdir -p .tmp

# Create a helper pom
# shellcheck disable=SC2016
echo '<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>org.neo4j</groupId>
	<artifactId>sdn-version-checker</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>demo</name>
	<description>Demo project for Spring Boot</description>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-dependencies</artifactId>
				<version>${bootVersion}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-neo4j</artifactId>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-dependency-plugin</artifactId>
				<version>3.8.1</version>
			</plugin>
		</plugins>
	</build>
</project>
' > .tmp/pom.xml


# Get all the dependencies
for sb in $(\
  xidel -se '/metadata/versioning/versions/version' https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot/maven-metadata.xml | \
  duckdb "$DB"  -noheader -csv -s "SELECT * from read_csv('/dev/stdin', header=false, columns={'spring_boot': 'VARCHAR'}) ANTI JOIN versions USING (spring_boot) WHERE f_make_version(spring_boot) >= [1,4,0] "\
); do
  set +e
  mvn -B -q -f .tmp/pom.xml -DoutputFile="$sb".json -DoutputType=json -DbootVersion="$sb" dependency:tree >/dev/null 2>/dev/null
  set -e
done

# Get release dates from the maven index, might not be 100% accurate, but good enough
curl -s https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot/ | sed -nE 's/.*title="(.*)\/".*([0-9]{4}-[0-9]{2}-[0-9]{2}).*/\1,\2/p' > .tmp/release_dates.csv

# Extract dependency information
if compgen -G ".tmp/*.json" > /dev/null;
then
duckdb "$DB" \
-s "
  WITH parent AS (
    SELECT unnest(children, recursive:=true) from read_json('.tmp/*.json')
  ), starter AS (
    SELECT version AS spring_boot, unnest(children, recursive:=true) FROM parent
  ), ogm AS (
    SELECT spring_boot, artifactId AS parent, version AS parentVersion, unnest(children, recursive:=true) FROM starter
  ), driver_via_ogm AS (
    SELECT spring_boot, artifactId AS parent, version AS parentVersion, unnest(children, recursive:=true) FROM ogm
  ), driver_via_sdn AS (
     SELECT spring_boot, artifactId AS parent, version AS parentVersion, unnest(children, recursive:=true) FROM starter
     WHERE starter.artifactId = 'spring-data-neo4j'
  )
  INSERT INTO versions BY NAME
  SELECT starter.spring_boot,
         r.release_date,
         starter.version AS spring_data_neo4j,
         ogm.version     AS neo4j_ogm,
         coalesce(driver_via_ogm.version, driver_via_sdn.version) AS neo4j_java_driver
  FROM starter
  NATURAL JOIN read_csv('.tmp/release_dates.csv', header=false, columns={'spring_boot': 'VARCHAR', 'release_date': 'DATE'}) r
  LEFT OUTER JOIN ogm ON
     ogm.parent = starter.artifactId AND
     ogm.parentVersion = starter.version AND
     ogm.spring_boot = starter.spring_boot AND
     ogm.artifactId = 'neo4j-ogm-core'
   LEFT OUTER JOIN driver_via_ogm ON
      driver_via_ogm.parent ='neo4j-ogm-bolt-driver' AND
      driver_via_ogm.parentVersion = ogm.version AND
      driver_via_ogm.spring_boot = starter.spring_boot AND
      driver_via_ogm.artifactId = 'neo4j-java-driver'
   LEFT OUTER JOIN driver_via_sdn ON
     driver_via_sdn.parent = starter.artifactId AND
     driver_via_sdn.parentVersion = starter.version AND
     driver_via_sdn.spring_boot = starter.spring_boot AND
     driver_via_sdn.artifactId = 'neo4j-java-driver'
  WHERE starter.artifactId = 'spring-data-neo4j'
  ON CONFLICT DO NOTHING
"
fi

rm -rf .tmp
