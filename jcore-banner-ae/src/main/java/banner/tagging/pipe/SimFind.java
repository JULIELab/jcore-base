/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging.pipe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * This class is used by the CRFTagger as the base for the distributional semantics features.
 * 
 * @author Siddhartha
 */
public class SimFind extends Pipe
{
	private static final long serialVersionUID = 1L;
	public HashMap<String, String> simFindStoreProteins ;
	
	public SimFind(String filename)
	{
		
		simFindStoreProteins = new HashMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("models/Thesarus_clinicalTrials_2000d_20.txt"));
			while (br.ready()) {
				String line = br.readLine();
				if (line.split("\\s").length > 1)
					simFindStoreProteins
					.put(line.split("\\s")[0].trim(), line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Instance pipe(Instance carrier)
	{

		TokenSequence ts = (TokenSequence) carrier.getData();
		for (int i = 0; i < ts.size(); i++)
		{
			Token t = ts.get(i);
			String lowercaseText = t.getText().toLowerCase();
			//String featureName = "";
			//do something here
			if(this.simFindStoreProteins.containsKey(lowercaseText)){
				String[] ret = this.simFindStoreProteins.get(lowercaseText).split("\\s");			
				for(int j=0; j<ret.length ; j++){
					t.setFeatureValue("SIMProt="+ret[j], 1.0);;
				}
			}
			t.getFeatures();
		}
		return carrier;
	}
}
