import os
from flair.models import SequenceTagger
from flair.data import Sentence
from typing import List

import sys

def decodeString(buffer):
    lengthBuffer = bytearray(4)
    buffer.readinto(lengthBuffer)
    length = int.from_bytes(lengthBuffer, 'big')
    content = bytearray(length)
    buffer.readinto(content)
    return content.decode("utf-8")

taggerPath = sys.argv[1]
tagger = SequenceTagger.load_from_file(taggerPath)

print("Ready for tagging.")
stdbuffer = sys.stdin.buffer
while True:
    line = decodeString(stdbuffer)
    if line.strip() == "exit":
        sys.exit(0)
    split = line.split("\t")
    sid      = split[0]
    sentence = Sentence(split[1])
    tagger.predict(sentence)
    for e in sentence.get_spans("ner"):
        tokenids = [t.idx for t in e.tokens]
        print(sid + "\t" + e.tag + "\t" + str(tokenids[0]) + "\t" + str(tokenids[-1]) + os.linesep)
    print("tagging finished")