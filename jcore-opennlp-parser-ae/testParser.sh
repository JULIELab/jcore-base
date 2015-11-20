#!/bin/bash
date
cd lib
export CLASSPATH=`for i in *.jar; do echo -n "lib/$i:";done;echo -n ":."`
cd ..
java -Xmx2000m -cp target/classes:$CLASSPATH opennlp.tools.lang.english.TreebankParser $* 
date
