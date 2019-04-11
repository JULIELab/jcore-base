import os
from flair.models import SequenceTagger
from flair.data import Sentence
from typing import List

from flair.embeddings import WordEmbeddings, CharacterEmbeddings, BytePairEmbeddings, FlairEmbeddings, BertEmbeddings, ELMoEmbeddings
from flair.embeddings import StackedEmbeddings

import sys
import json
from struct import *
import time


def decodeString(buffer):
    lengthBuffer = bytearray(4)
    buffer.readinto(lengthBuffer)
    length = int.from_bytes(lengthBuffer, 'big')
    content = bytearray(length)
    buffer.readinto(content)
    return content.decode("utf-8")

embeddingList=[]
for i in range(1,len(sys.argv)):
    arg = sys.argv[i]
    typeAndPath = arg.split(":")
    type = typeAndPath[0]
    path = typeAndPath[1]
    if type == "word":
        embeddingList.append(WordEmbeddings(path))
    if type == "char":
        embeddingList.append(CharacterEmbeddings(path))
    if type == "bytepair":
        embeddingList.append(BytePairEmbeddings(path))
    if type == "flair":
        embeddingList.append(FlairEmbeddings(path))
    if type == "bert":
        embeddingList.append(BertEmbeddings(path))
    if type == "elmo":
        embeddingList.append(ELMoEmbeddings(path))
if len(embeddingList) > 1:
    embeddings = StackedEmbeddings(embeddings=embeddingList)
else:
    embeddings = embeddingList[0]

stdbuffer = sys.stdin.buffer
print("Script is ready")
while True:
    line = decodeString(stdbuffer)
    if line.strip() == "exit":
        sys.exit(0)
    overalltime = time.time()
    sentenceTaggingRequests = json.loads(line)
    # The format contract for the response is:
    # 1. The total number of bytes sent (including 2. and 3.)
    # 2. The number of vectors returned
    # 3. The length (dimensionality) of the vectors
    # 4. The encoded double vectors

    # In this byte array, all vectors from all sentences will be encoded
    ba = bytearray()

    # We collect all sentences in one list in order to compute all embeddings in one call.
    # This is important for contextualized embeddings.
    sentences = []
    numReturnedVectors = 0
    # Compute the number of vectors returned. We need to do this beforehand
    # so we can then write the vectors directly into the byte array (see the format description above).
    for sentenceTaggingRequest in sentenceTaggingRequests:
        # Read the JSON data: The sentence and potentially the indices of tokens for which embedding vectors
        # should be returned
        sentence = Sentence(sentenceTaggingRequest['sentence'])
        sentences.append(sentence)
        tokenIndicesToReturn = []
        if 'tokenIndicesToReturn' in sentenceTaggingRequest.keys():
            tokenIndicesToReturn = sentenceTaggingRequest['tokenIndicesToReturn']
        if len(tokenIndicesToReturn) == 0:
            numReturnedVectors = numReturnedVectors + len(sentence)
        else:
            numReturnedVectors = numReturnedVectors + len(tokenIndicesToReturn)

    # 2. Write the number of vectors into the output
    ba.extend(pack('>i', numReturnedVectors))

    # Now compute the vectors
    # This does the actual embedding vector computation
    runtime = time.time()
    embeddings.embed(sentences)
    runtime = time.time() - runtime
    #print("flair embedding computation time:", runtime, file=sys.stderr)

    # 3. Get the vectorlength and write it into the output byte array
    vectorlength = len(sentences[0][0].embedding)
    doubleformat = '>' + 'd'*vectorlength
    ba.extend(pack('>i', vectorlength))

    # Now iterate through the sentences and write the output
    for i,sentenceTaggingRequest in enumerate(sentenceTaggingRequests):
        tokenIndicesToReturn = []
        if 'tokenIndicesToReturn' in sentenceTaggingRequest.keys():
            tokenIndicesToReturn = sentenceTaggingRequest['tokenIndicesToReturn']

        sentence = sentences[i]

        # 4. Write the actual vectors
        for i,token in enumerate(sentence):
            if len(tokenIndicesToReturn) == 0:
                ba.extend(pack(doubleformat, *token.embedding.numpy()))
            elif i in tokenIndicesToReturn:
                ba.extend(pack(doubleformat, *token.embedding.numpy()))

    # 1. Write the length of the complete data to the output stream
    messagelength = pack('>i', len(ba))
    sendtime = time.time()
    sys.stdout.buffer.write(messagelength)
    sys.stdout.buffer.write(ba)
    sendtime = time.time() - sendtime
    #print("embedding sending time:", sendtime, file=sys.stderr)
    print(end='')
    overalltime = time.time() - overalltime
    #print("overall script processing time:", overalltime, file=sys.stderr)