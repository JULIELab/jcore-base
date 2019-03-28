package banner.tagging.pipe;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChemicalSuffix extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3442366946904548L;
	private String prefix;

	public ChemicalSuffix(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public Instance pipe(Instance inst) {
		TokenSequence ts = (TokenSequence) inst.getData();
		for (Token t : ts) {
			String tokenText = t.getText();
			String chemPreSuf = null;
			if (tokenText.matches(".*(yl|ylidyne|oyl|sulfonyl)")) {
				chemPreSuf = "-CHEMinlineSuffix-";
			} else if (tokenText.matches("(meth|eth|prop|tetracos).*")) {
				chemPreSuf = "-CHEMalkaneStem-";
			} else if (tokenText.matches("(di|tri|tetra).*")) {
				chemPreSuf = "-CHEMsimpleMultiplier-";
			} else if (tokenText.matches("(benzen|pyridin|toluen).*")) {
				chemPreSuf = "-CHEMtrivialRing-";
			} else if (tokenText.matches(
					".*(one|ol|carboxylic|amide|ate|acid|ium|ylium|ide|uide|iran|olan|inan|pyrid|acrid|amid|keten|formazan|fydrazin)(s|)")) {
				chemPreSuf = "-CHEMsuffix-";
			}
			if (chemPreSuf != null) {
				t.setFeatureValue(prefix+chemPreSuf, 1.0);
			}
		}
		return inst;
	}

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeObject(prefix);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		prefix = (String)in.readObject();
	}
	
}
