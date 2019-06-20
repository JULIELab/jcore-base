import os
from flair.models import SequenceTagger
from flair.data import Sentence
from typing import List

from struct import *

import sys

# The input comes as a byte array. The bytes first contain
# the length of the message that follows. The message itself is
# just the sentence to tag (with tokens separated by whitespace)
# in UTF-8 encoding.
def decodeString(buffer):
    lengthBuffer = bytearray(4)
    buffer.readinto(lengthBuffer)
    length = int.from_bytes(lengthBuffer, 'big')
    content = bytearray(length)
    buffer.readinto(content)
    return content.decode("utf-8")

taggerPath = sys.argv[1]
# Possible values: ALL, ENTITIES, NONE
sendEmbeddings = sys.argv[2]
tagger = SequenceTagger.load(taggerPath)

print("Ready for tagging.")
stdbuffer = sys.stdin.buffer
while True:
    # Sentence input
    line = decodeString(stdbuffer)
    if line.strip() == "exit":
        sys.exit(0)
    split = line.split("\t")
    sid      = split[0]
    sentence = Sentence(split[1])
    # NER tagging
    clearWordEmbeddings = sendEmbeddings == "NONE"
    tagger.predict(sentence, clear_word_embeddings=clearWordEmbeddings)

    # Resonse

    # In this byte array, all entities and all vectors from the sentence will be encoded
    ba = bytearray()

    numReturnedVectors = 0
    taggedEntities = []
    embeddings = []
    for e in sentence.get_spans("ner"):
        tokenids = [t.idx for t in e.tokens]
        # Store sentence ID, token ID and the embedding
        if sendEmbeddings != "NONE":
            embeddings.extend([(sid, i, sentence.tokens[i-1].embedding.numpy()) for i in tokenids])
        numReturnedVectors = len(embeddings)
        taggedEntities.append(sid + "\t" + e.tag + "\t" + str(tokenids[0]) + "\t" + str(tokenids[-1]))


    ba.extend(pack('>i', len(taggedEntities)))
    for taggedEntity in taggedEntities:
        taggedEntityBytes = bytes(taggedEntity, 'utf-8')
        ba.extend(pack('>i', len(taggedEntityBytes)))
        ba.extend(taggedEntityBytes)
    # 2. Write the number of vectors into the output
    ba.extend(pack('>i', numReturnedVectors))
    # 3. Get the vectorlength and write it into the output byte array
    vectorlength = 0 if len(embeddings) == 0 else len(embeddings[0][2])
    doubleformat = '>' + 'd'*vectorlength
    ba.extend(pack('>i', vectorlength))

    # 4. Write the actual vectors. The "embeddings" contain pairs
    # of token ID (1-based) and the actual vector.
    for triple in embeddings:
        sentenceIdBytes = bytes(triple[0], 'utf-8')
        ba.extend(pack('>i', len(sentenceIdBytes)))
        ba.extend(sentenceIdBytes)
        ba.extend(pack('>i', triple[1]))
        ba.extend(pack(doubleformat, *triple[2]))

    sys.stdout.buffer.write(pack('>i', len(ba)))
    sys.stdout.buffer.write(ba)

    #print("tagging finished")