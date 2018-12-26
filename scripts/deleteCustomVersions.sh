#!/bin/bash
# To executed in the project's root directory.
# This script deleted the <version> element from all artifact's pom.xml files. Not the parent or the dependency verions,
# only the version tag of the artifact itself so that the version is inherited from the parent.
# This script is useful when creating a clean JCoRe release where all components have the exact same version number
# given by the jcore-base parent POM.

if [ ! -f julie-xml-tools.jar ]; then
	wget https://oss.sonatype.org/content/repositories/releases/de/julielab/julie-xml-tools/0.4.3/julie-xml-tools-0.4.3-xml-tools-assembly.jar -O julie-xml-tools.jar
fi

find . -mindepth 2 -name 'pom.xml' | xargs -n1 -I{} java -cp julie-xml-tools.jar de.julielab.xml.ElementDeleter {} /project/version
