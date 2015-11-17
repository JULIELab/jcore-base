# JCoRe Base

Base modules of JCoRe as described in:

[to do]


### Maven Artifacts

If not stated otherwise, all the components found in this project are in their release version also available as Maven artifacts:
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
The version variable `${jcore-version}` is defined in the jcore-parent pom and should not be edited manually, as it ensures compatibility.
