# JCoRe Named Entity Tagger Analysis Engine
Tagger for automatically detecting and classifying Named Entity Mentions in written plain text

### Manual
data.ppd means data in socalled "piped format" which looks the following:
`<token1>|<POS>|<label><token2>|<POS>|<label> <token3>|<POS>|<label> ...`
One sentence per line.

Running JNET consumes data in piped format which can be produced from IOB and
further meta data, such as PoS information, by using the FormatConverter class
in JNET. This may be done by running JNET with the f parameter.

Remember to use the correct <name>.tags file which is needed in almost all
actions performed by JNET.

For comparing gold standard with the predictions made by the tagger, just
use the mode "c" and give both the gold standard and the predictions in IOB
format (make sure, both files have the same length, i.e. especially an empty
line at the end and a line break).

JNET may be run by giving console commands in the following form:

runJNET.sh <parameters>

If you want to see which modes JNET provides just leave the parameters blank.
