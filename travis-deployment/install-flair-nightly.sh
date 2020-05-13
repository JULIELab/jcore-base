#!/usr/bin/env bash

if [ ! -d "flair" ]; then
    git clone https://github.com/khituras/flair.git
    cd flair
    $PYTHON -m pip install setuptools
    sudo -H $PYTHON setup.py install
fi