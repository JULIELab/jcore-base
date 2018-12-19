#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
	echo "Executing deploy"
	wget https://search.maven.org/remotecontent?filepath=de/julielab/julie-xml-tools/0.4.2/julie-xml-tools-0.4.2-xml-tools-assembly.jar --output-document julie-xml-tools.jar


	for i in `java -jar julie-xml-tools.jar pom.xml //module`; do
	    java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.GetCoordinatesFromPom $i/pom.xml > coords.txt;
	    groupId=`grep 'GROUPID:' coords.txt | sed 's/^GROUPID: //'`
	    artifactId=`grep 'ARTIFACTID:' coords.txt | sed 's/^ARTIFACTID: //'`
	    version=`grep 'VERSION:' coords.txt | sed 's/^VERSION: //'`
	    if [ `java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.FindRemoteChecksum` ]; then

    done
#    mvn deploy -P sonatype-nexus-deployment --settings travis-deployment/mvnsettings.xml -DskipTests=true
else
	echo "Deploy not executed"
fi
