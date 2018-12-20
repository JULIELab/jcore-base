#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
	echo "Executing deploy"
	if [ ! -f julie-xml-tools.jar ]; then
	    wget https://search.maven.org/remotecontent?filepath=de/julielab/julie-xml-tools/0.4.2/julie-xml-tools-0.4.2-xml-tools-assembly.jar --output-document julie-xml-tools.jar
	fi
	if [ ! -f julie-xml-tools.jar ]; then
    	    # TODO: Download the aether tools
    fi


	for i in `java -jar julie-xml-tools.jar pom.xml //module`; do
	    java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.GetCoordinatesFromRawPom $i/pom.xml > coords.txt;
	    groupId=`grep 'GROUPID:' coords.txt | sed 's/^GROUPID: //'`
	    artifactId=`grep 'ARTIFACTID:' coords.txt | sed 's/^ARTIFACTID: //'`
	    version=`grep 'VERSION:' coords.txt | sed 's/^VERSION: //'`
	    packaging=`grep 'PACKAGING:' coords.txt | sed 's/^PACKAGING: //'`
	    artifactFile=$i/target/$artifactId-$version.$packaging
	    if [ ! -f $artifactFile ]; then
	        echo "Could not find the expected artifact file $artifactFile. Has the project successfully been built?"
	    else
	        checksum=`md5 -r $artifactFile | grep -io '^[0-9a-z]*'`
	        echo "Trying to find MD5 checksum $checksum of artifact $groupId:$artifactId:$packaging:$version"
	        csFound=`java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.FindRemoteChecksum $groupId:$artifactId:$packaging:$version $checksum`
            if [ "$csFound" == "true" ]; then
                echo "Found the checksum, not deploying again."
            else
                echo "Checksum was not found, deploying the artifact."
                mvn deploy -P sonatype-nexus-deployment --settings travis-deployment/mvnsettings.xml -DskipTests=true
            fi
	    fi
    done
else
	echo "Deploy not executed"
fi
