#assumes current executable version of jpos 
(
cd ../../../target
java -jar jpos-2.3.0-SNAPSHOT-jar-with-dependencies.jar t ../src/test/resources/testModelTrainingMaterial ../src/test/resources/testModel ../src/main/resources/defaultFeatureConf.conf 1
)
