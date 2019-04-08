import os
from flair.models import SequenceTagger
from flair.data import Sentence
from typing import List

import sys

taggerPath = sys.argv[1]
tagger = SequenceTagger.load_from_file(taggerPath)

for line in sys.stdin:
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