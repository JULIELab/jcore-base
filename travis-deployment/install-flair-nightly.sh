#!/usr/bin/env bash

if [ ! -d "flair" ]; then
    git clone https://github.com/khituras/flair.git
    cd flair
    python setup.py install
fi