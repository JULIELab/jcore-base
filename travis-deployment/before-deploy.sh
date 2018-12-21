#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_2a2291398afc_key -iv $encrypted_2a2291398afc_iv -in codesigning.asc.enc -out travis-deployment/codesigning.asc -d
    if [ ! "$?" -eq "0" ]; then
        echo "Could not decrypt gpg key";
        exit 1;
    fi
	gpg --fast-import travis-deployment/codesigning.asc
	if [ ! "$?" -eq "0" ]; then
            echo "Could not import GPG signing key";
            exit 2;
        fi
	gpg -k
	gpg -K
else
    echo "WARNING: The conditions for gpg key import were not met, no gpg signing key will be available for deploy."
fi

