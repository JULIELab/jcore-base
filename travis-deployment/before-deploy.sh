#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
	openssl aes-256-cbc -K $encrypted_a53075a80cca_key -iv $encrypted_a53075a80cca_iv -in travis-deployment/codesigning.asc.enc -out travis-deployment/codesigning.asc -d
	gpg --fast-import travis-deployment/codesigning.asc
fi

