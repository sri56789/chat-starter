#!/bin/bash
# Run the Spring Boot application with increased memory
export MAVEN_OPTS="-Xmx2g -Xms512m"
mvn spring-boot:run



