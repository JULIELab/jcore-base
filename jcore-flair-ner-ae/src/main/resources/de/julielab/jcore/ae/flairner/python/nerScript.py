import os
from flair.models import SequenceTagger
from flair.data import Sentence
from typing import List

import json
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
    sentenceTaggingRequests = json.loads(line)
    taggedEntities = []
    embeddings = []
    for sentenceToTag in sentenceTaggingRequests:
        sid      = sentenceToTag['sid']
        sentence = Sentence(sentenceToTag['text'])
        # NER tagging
        clearWordEmbeddings = sendEmbeddings == "NONE"
        tagger.predict(sentence, clear_word_embeddings=clearWordEmbeddings)

        # Response

        # In this byte array, all entities and all vectors from the sentence will be encoded
        ba = bytearray()

        for e in sentence.get_spans("ner"):
            tokenids = [t.idx for t in e.tokens]
            # Store sentence ID, token ID and the embedding
            if sendEmbeddings == "ENTITIES":
                embeddings.extend([(sid, i, sentence.tokens[i-1].embedding.numpy()) for i in tokenids])
            taggedEntities.append(sid + "\t" + e.tag + "\t" + str(tokenids[0]) + "\t" + str(tokenids[-1]))

        if sendEmbeddings == "ALL":
            for i, token in enumerate(sentence.tokens):
                embeddings.append((sid, i+1, token.embedding.numpy()))

    ba.extend(pack('>i', len(taggedEntities)))
    for taggedEntity in taggedEntities:
        taggedEntityBytes = bytes(taggedEntity, 'utf-8')
        ba.extend(pack('>i', len(taggedEntityBytes)))
        ba.extend(taggedEntityBytes)
    # 2. Write the number of vectors into the output
    ba.extend(pack('>i', len(embeddings)))
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