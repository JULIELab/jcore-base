# JCoRe Base

Base modules of JCoRe as described in:

```[to do]```

### Objective
These are the base components of the JULIE Lab Component Repository (JCoRe), an open software repository for full-scale natural language processing based on the UIMA middleware framework. JCoRe offers a broad range of text analytics (mostly) for English-language scientific abstracts and full-text articles, especially for the biology domain.
In order to automate the builds of complex NLP pipelines and properly represent and track dependencies of the underlying Java code, all our components are also available as artifacts from Maven Central.
A descrition for each individual component can be found in their respective `README.md`.

### Dependencies
In order to use our components you need at least [Java 7](http://java.com/en/), [UIMA 2.6](https://uima.apache.org/index.html) & [Maven 3.0](https://maven.apache.org/) or higher.

### UIMA's Collection Processing Engine (CPE)
For more detailed information see [UIMA's CPE Documentation](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tutorials_and_users_guides/tutorials_and_users_guides.html#ugr.tug.cpe)

### Maven Artifacts
If not stated otherwise, all the components found in this project are at least in their latest release version also available as Maven artifacts:
```
<dependency>
    <groupId>de.julielab</groupId>
    <artifactId>#COMPONENT-NAME</artifactId>
    <version>${jcore-version}</version>
</dependency>
```
For instance, to get the BioSEM Annotator, include this in your Maven dependencies:
```
<dependency>
    <groupId>de.julielab</groupId>
    <artifactId>jcore-biosem-ae</artifactId>
    <version>${jcore-version}</version>
</dependency>
```
The version variable `${jcore-version}` is defined in the jcore-parent pom and should not be edited manually, as it ensures compatibility. An exemption from this rule are bugfix version, which might also be available from Maven Central. If for instance we deployed a first bugfix version for the `jcore-biosem-ae` from the `2.0.0` release, the Maven coordinates would be the following:
```
<dependency>
    <groupId>de.julielab</groupId>
    <artifactId>jcore-biosem-ae</artifactId>
    <version>2.0.1</version>
</dependency>
```
