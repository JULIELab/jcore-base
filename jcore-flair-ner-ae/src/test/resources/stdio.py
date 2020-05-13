'''
A very simple program that just repeats its input and ends when
the "exit" line is sent.
'''
import sys
import time
import flair
for line in sys.stdin:
    if line.strip() == "exit":
        sys.exit(0)
    print("Got line: " + line.strip())

