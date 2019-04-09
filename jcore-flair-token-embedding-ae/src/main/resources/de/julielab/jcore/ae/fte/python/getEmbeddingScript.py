import os
from flair.models import SequenceTagger
from flair.data import Sentence
from typing import List

from flair.embeddings import WordEmbeddings, CharacterEmbeddings, BytePairEmbeddings, FlairEmbeddings, BertEmbeddings, ELMoEmbeddings
from flair.embeddings import StackedEmbeddings

import sys
import json


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
        embeddingList.extend(WordEmbeddings(path))
    if type == "char":
        embeddingList.extend(CharacterEmbeddings(path))
    if type == "bytepair":
        embeddingList.extend(BytePairEmbeddings(path))
    if type == "flair":
        embeddingList.extend(FlairEmbeddings(path))
    if type == "bert":
        embeddingList.extend(BertEmbeddings(path))
    if type == "elmo":
        embeddingList.extend(ELMoEmbeddings(path))
if len(embeddingList) > 1:
    embeddings = StackedEmbeddings(embeddings=embeddingList)
else:
    embeddings = embeddingList[0]

stdbuffer = sys.stdin.buffer
while True:
    line = decodeString(stdbuffer)
    if line.strip() == "exit":
        sys.exit(0)
    for sentenceTaggingRequest in json.loads(line);
        sentence = Sentence(sentenceTaggingRequest['sentence'])
        tokenIndicesToReturn = []
        if 'tokenIndicesToReturn' in sentenceTaggingRequest.keys():
            tokenIndicesToReturn = sentenceTaggingRequest['tokenIndicesToReturn']
        embeddings.embed(sentence)
        for token in sentence:
            print(token)
            print(token.embedding)