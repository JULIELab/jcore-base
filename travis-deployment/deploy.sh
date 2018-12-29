#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
	echo "Executing deploy"
	if [ ! -f julie-xml-tools.jar ]; then
	    wget https://search.maven.org/remotecontent?filepath=de/julielab/julie-xml-tools/0.4.2/julie-xml-tools-0.4.2-xml-tools-assembly.jar --output-document julie-xml-tools.jar
	fi
	if [ ! -f julielab-maven-aether-utilities.jar ]; then
    	    wget https://oss.sonatype.org/content/repositories/snapshots/de/julielab/julielab-maven-aether-utilities/1.0.0-SNAPSHOT/julielab-maven-aether-utilities-1.0.0-20181221.093634-2-cli-assembly.jar --output-document julielab-maven-aether-utilities.jar
    fi


	for i in . `java -jar julie-xml-tools.jar pom.xml //module`; do
	    java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.GetCoordinatesFromRawPom $i/pom.xml > coords.txt;
	    groupId=`grep 'GROUPID:' coords.txt | sed 's/^GROUPID: //'`
	    artifactId=`grep 'ARTIFACTID:' coords.txt | sed 's/^ARTIFACTID: //'`
	    version=`grep 'VERSION:' coords.txt | sed 's/^VERSION: //'`
	    packaging=`grep 'PACKAGING:' coords.txt | sed 's/^PACKAGING: //'`
	    artifactFile=$i/target/$artifactId-$version.$packaging
        echo "Checking if $groupId:$artifactId:$packaging:$version exists"
        csNotFound=`java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.GetRemoteChecksums $groupId:$artifactId:$packaging:$version | grep '<checkums not found>'`
	    if [[ $version =~ .*SNAPSHOT.* ]] || [ "$csNotFound" == "<checkums not found>" ]; then
            echo "This is a SNAPSHOT or a release that has not yet been deployed. Deploying."
            mvn deploy -B -f $i/pom.xml -P sonatype-nexus-deployment --settings travis-deployment/mvnsettings.xml -DskipTests=true -N
	    fi
    done

    echo "Now deploying JeDIS components"
    for i in . `java -jar julie-xml-tools.jar jedis-parent/pom.xml //module`; do
            path=`echo $i | sed 's|../||'
    	    java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.GetCoordinatesFromRawPom $path/pom.xml > coords.txt;
    	    groupId=`grep 'GROUPID:' coords.txt | sed 's/^GROUPID: //'`
    	    artifactId=`grep 'ARTIFACTID:' coords.txt | sed 's/^ARTIFACTID: //'`
    	    version=`grep 'VERSION:' coords.txt | sed 's/^VERSION: //'`
    	    packaging=`grep 'PACKAGING:' coords.txt | sed 's/^PACKAGING: //'`
    	    artifactFile=$path/target/$artifactId-$version.$packaging
            echo "Checking if $groupId:$artifactId:$packaging:$version exists"
            csNotFound=`java -cp julielab-maven-aether-utilities.jar de.julielab.utilities.aether.apps.GetRemoteChecksums $groupId:$artifactId:$packaging:$version | grep '<checkums not found>'`
    	    if [[ $version =~ .*SNAPSHOT.* ]] || [ "$csNotFound" == "<checkums not found>" ]; then
                echo "This is a SNAPSHOT or a release that has not yet been deployed. Deploying."
                mvn deploy -B -f $path/pom.xml -P sonatype-nexus-deployment --settings travis-deployment/mvnsettings.xml -DskipTests=true -N
    	    fi
        done
else
	echo "Deploy not executed"
fi
