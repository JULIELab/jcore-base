package banner.tagging.pipe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenLength extends Pipe
{

	private static final long serialVersionUID = -8834281180687367486L;

	private String prefix;
	private Pattern matchingRegex;

	public TokenLength(String prefix, Pattern matchingRegex)
	{
		this.prefix = prefix;
		this.matchingRegex = matchingRegex;
	}

	public TokenLength(String prefix)
	{
		this.prefix = prefix;
		this.matchingRegex = null;
	}

	@SuppressWarnings("unused")
	private TokenLength()
	{
		// Empty
	}

	public Instance pipe(Instance carrier)
	{
		TokenSequence ts = (TokenSequence)carrier.getData();
		for (int i = 0; i < ts.size(); i++)
		{
			Token t = ts.get(i);
			String text = Integer.toString(t.getText().length());
			if (matchingRegex == null || matchingRegex.matcher(text).matches())
			{
				String featureName = null;
				if (prefix == null)
					featureName = text;
				else
					featureName = prefix + text;
				t.setFeatureValue(featureName, 1.0);
			}
		}
		return carrier;
	}

	// Serialization

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeObject(prefix);
		out.writeObject(matchingRegex);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		prefix = (String)in.readObject();
		matchingRegex = (Pattern)in.readObject();
	}
}
