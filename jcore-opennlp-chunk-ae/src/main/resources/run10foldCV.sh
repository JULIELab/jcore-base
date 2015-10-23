#!/bin/bash

# This script receives a file containing openNLP chunker training data,
# splits it into 10 test-training pairs and runs a 10 fold cross validation

# $1 complete chunk data, e.g. /data/data_corpora/genomics/Genia/GENIA_treebank_v1_openNLPChunk
# $2 output data directory, e.g. /home/rubruck/Documents/10foldCVData (this data is not needed anymore after Evaluation is done)
# ./run10foldCV.sh /data/data_corpora/genomics/Genia/GENIA_treebank_v1_openNLPChunk /home/rubruck/Documents/10foldCVData

# Correct number of arguments passed to script?
if [ $# -ne 2 ]
then
  echo "Usage: prepare10foldCV inputFile outputDirectory"
  exit
fi

outputDir=$2;

# Check if file exists
if [ ! -f ${inputFile} ]       
then
  echo "File ${inputFile} does not exist."
  exit
fi

# Check if output directory exists
if [ -d ${outputDir} ]
then
	echo "Output Directory ${outputDir} already exists."
	echo "If you want it to be overwritten type \"yes\" and press [ENTER], else \"no\" to end script"
        answerStatus=0
        while [ $answerStatus -eq 0 ]; do
                read answer
                if [[ $answer == "no" ]]; then
                        exit
                elif [[ $answer == "yes" ]]; then
                        answerStatus=1 
						# delete output directory
                        rm -r ${outputDir}
                        mkdir -p ${outputDir}
                else
                        echo "State either yes or no"
                fi
        done
else
        mkdir -p ${outputDir}
fi

# first divide the file into 10 temporary files of equal length
numberOfLines=$(wc -l < $1)
echo "${numberOfLines} lines divided into 10 equal parts..."

linesPerFile=$(($numberOfLines / 10))
echo "splitting into ${linesPerFile} lines per file..."

# creates xaa, xab, ..., xaj and xak, in case that there are some lines left over
split -l ${linesPerFile} $1
rm xak

# training files
echo "creating training files ..."
cat xab xac xad xae xaf xag xah xai xaj > ${outputDir}/train1
cat xaa xac xad xae xaf xag xah xai xaj > ${outputDir}/train2
cat xaa xab xad xae xaf xag xah xai xaj > ${outputDir}/train3
cat xaa xab xac xae xaf xag xah xai xaj > ${outputDir}/train4
cat xaa xab xac xad xaf xag xah xai xaj > ${outputDir}/train5
cat xaa xab xac xad xae xag xah xai xaj > ${outputDir}/train6
cat xaa xab xac xad xae xaf xah xai xaj > ${outputDir}/train7
cat xaa xab xac xad xae xaf xag xai xaj > ${outputDir}/train8
cat xaa xab xac xad xae xaf xag xah xaj > ${outputDir}/train9
cat xaa xab xac xad xae xaf xag xah xai > ${outputDir}/train10

# test files
echo "creating test files ..."
mv xaa ${outputDir}/test1
mv xab ${outputDir}/test2
mv xac ${outputDir}/test3
mv xad ${outputDir}/test4
mv xae ${outputDir}/test5
mv xaf ${outputDir}/test6
mv xag ${outputDir}/test7
mv xah ${outputDir}/test8
mv xai ${outputDir}/test9
mv xaj ${outputDir}/test10

# train models
echo "training ten models for 10-fold-cross-validation..."
echo "this will take some time."
echo "training model 1 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model1.bin -lang en -data ${outputDir}/train1 -encoding UTF-8
echo "training model 2 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model2.bin -lang en -data ${outputDir}/train2 -encoding UTF-8
echo "training model 3 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model3.bin -lang en -data ${outputDir}/train3 -encoding UTF-8
echo "training model 4 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model4.bin -lang en -data ${outputDir}/train4 -encoding UTF-8
echo "training model 5 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model5.bin -lang en -data ${outputDir}/train5 -encoding UTF-8
echo "training model 6 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model6.bin -lang en -data ${outputDir}/train6 -encoding UTF-8
echo "training model 7 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model7.bin -lang en -data ${outputDir}/train7 -encoding UTF-8
echo "training model 8 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model8.bin -lang en -data ${outputDir}/train8 -encoding UTF-8
echo "training model 9 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model9.bin -lang en -data ${outputDir}/train9 -encoding UTF-8
echo "training model 10 of 10..."
opennlp ChunkerTrainerME -model ${outputDir}/model10.bin -lang en -data ${outputDir}/train10 -encoding UTF-8

# evaluation
echo "running 10-fold-cross validation evaluation..."
opennlp ChunkerEvaluator -model ${outputDir}/model1.bin -data ${outputDir}/test1 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model2.bin -data ${outputDir}/test2 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model3.bin -data ${outputDir}/test3 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model4.bin -data ${outputDir}/test4 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model5.bin -data ${outputDir}/test5 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model6.bin -data ${outputDir}/test6 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model7.bin -data ${outputDir}/test7 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model8.bin -data ${outputDir}/test8 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model9.bin -data ${outputDir}/test9 -encoding UTF-8
opennlp ChunkerEvaluator -model ${outputDir}/model10.bin -data ${outputDir}/test1 -encoding UTF-8
