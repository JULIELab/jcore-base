#!/bin/bash
# To be executed from the root directory.
# Uses the createMetaDescriptors.py python script (copied from jcore-misc) to create
# the component meta descriptors

find . -type d -name 'jcore-*' -depth 1 | xargs -n1 -I{} scripts/createMetaDescriptors.py -c {}
