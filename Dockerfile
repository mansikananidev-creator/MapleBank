# ---- Build stage ----
# Compiles the app with the exact Maven wrapper/version the project uses, in a
# throwaway container so nobody needs Maven installed locally.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only what's needed to resolve dependencies first, so this layer is cached
# and only re-runs when pom.xml actually changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# ---- Run stage ----
# Slim JRE-only image - no build tooling, smaller attack surface, smaller image.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
