package banner.tagging.pipe;

import java.io.File;
import java.io.IOException;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import de.julielab.ml.embeddings.client.WordEmbeddingClient;
import de.julielab.ml.embeddings.spi.WordEmbedding;

public class WordEmbeddingPipe extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = 598896430261992797L;

	private String featureName;

	private WordEmbedding embeddings;

	public WordEmbeddingPipe(String prefix) throws IOException {
		this.featureName = prefix;
		embeddings = new WordEmbeddingClient("localhost", 4567);
	}

	@Override
	public Instance pipe(Instance inst) {
		return meanOverAllWords(inst);
	}

	private Instance meanOverAllWords(Instance carrier) {
		TokenSequence data = (TokenSequence) carrier.getData();
		for (int i = 0; i < data.size(); ++i) {
			Token t = data.get(i);
			double[] wordVector = embeddings.getWordVector(t.getText());
			if (wordVector.length > 0) {
				for (int j = 0; j < wordVector.length; j++) {
					t.setFeatureValue(featureName + j, wordVector[j]);
				}
			}
		}
		return carrier;

	}

}
