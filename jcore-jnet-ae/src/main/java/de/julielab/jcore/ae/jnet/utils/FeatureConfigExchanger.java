/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.jnet.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import de.julielab.jnet.tagger.NETagger;

public class FeatureConfigExchanger {

	public static void main(final String[] args) throws FileNotFoundException,
			IOException, ClassNotFoundException {
		if (args.length != 3) {
			System.err
					.println("Usage: FeatureConfigExchanger <model file> <new model file> <new feature config file>");
			System.exit(-1);
		}
		final String orgModelName = args[0];
		final String newModelName = args[1];
		final String newFeatureConfig = args[2];
		final NETagger tagger = new NETagger();
		tagger.readModel(new FileInputStream(orgModelName));
		final Properties featureConfig = new Properties();
		featureConfig.load(new FileInputStream(newFeatureConfig));
		tagger.setFeatureConfig(featureConfig);
		tagger.writeModel(newModelName);

		System.out.println("wrote model with new feature config to: "
				+ newModelName);
	}
}
