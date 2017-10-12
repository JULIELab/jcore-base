package banner.tagging.pipe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class ProteinSymbols extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7211355932042290296L;
	private String prefix;

	public ProteinSymbols(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Instance pipe(Instance inst) {
		TokenSequence ts = (TokenSequence) inst.getData();
		for (Token t : ts) {
			String tokenText = t.getText();
			String proteinSym = null;
			if (tokenText.matches(
					".*(glutamine|glutamic|leucine|valine|isoleucine|lysine|alanine|glycine|aspartate|methionine|threonine|histidine|aspartic|asparticacid|arginine|asparagine|tryptophan|proline|phenylalanine|cysteine|serine|glutamate|tyrosine|stop|frameshift).*")) {
				proteinSym = "-ProteinSymFull-";
			} else if (tokenText.matches(
					"(cys|ile|ser|gln|met|asn|pro|lys|asp|thr|phe|ala|gly|his|leu|arg|trp|val|glu|tyr|fs|fsx)")) {
				proteinSym = "-ProteinSymTri-";
			} else if (tokenText.matches("[CISQMNPKDTFAGHLRWVEYX]")) {
				proteinSym = "-ProteinSymChar-";
			}

			if (proteinSym != null) {
				t.setFeatureValue(prefix + proteinSym, 1.0);
			}
		}

		return inst;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(prefix);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		prefix = (String) in.readObject();
	}

}
