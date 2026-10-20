# Etape 1 : compilation de l'application avec Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# On copie d'abord le pom pour profiter du cache des dependances
COPY pom.xml .
RUN mvn dependency:go-offline

# Puis le code source et on construit le jar
COPY src ./src
RUN mvn clean package -DskipTests

# Etape 2 : execution de l'application avec un JRE leger
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/atlasrh-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
