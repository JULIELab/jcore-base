package de.julielab.jcore.ae.jemas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class EmotionLexicon {
	
	HashMap<String,Emotion> map = new HashMap<String, Emotion>();

	public EmotionLexicon (String path) {
		File f = new File(path);
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			while ((line = br.readLine()) != null){		
				if (!line.startsWith("//")) {
					String[] parts = line.split("\\t");
					Emotion emo = new Emotion(Double.parseDouble(parts[1]),
							Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
					map.put(parts[0], emo);
				}
			}
			br.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage()); 
		}
//		//debug
//		this.printLexcion();
	}
		
	public Emotion get (String entry) {
		return map.get(entry);
	}
	
	public void printLexcion() {
		for (String key:map.keySet()) {
			Emotion emo = map.get(key);
			System.err.println(key +  ": "+ emo.getValence() + ", "+ emo.getArousal()+ ", " + emo.getDominance());
		}
	}

			
		
		
	
}
