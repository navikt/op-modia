No longer in use

# Oppfølginsplan-Modia

Repository for op-modia. Application written in Kotlin used to receive oppfølginsplan from kafka topic,
then pushing it to Modia.

### Technologies & Tools

* Kotlin
* Gradle
* Ktor
* Spek

### Getting started
# Build and run tests
./gradlew clean build

### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run `./gradlew shadowJar` or  on windows 
`gradlew.bat shadowJar`
