package banner.tagging.pipe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenWordClass extends Pipe
{

	private static final long serialVersionUID = 8996255757211528445L;

	String prefix;
	Pattern matchingRegex;
	boolean brief;

	public TokenWordClass(String prefix, Pattern matchingRegex, boolean brief)
	{
		this.prefix = prefix;
		this.matchingRegex = matchingRegex;
		this.brief = brief;
	}

	public TokenWordClass(String prefix, boolean brief)
	{
		this.prefix = prefix;
		this.matchingRegex = null;
		this.brief = brief;
	}

	@SuppressWarnings("unused")
	private TokenWordClass()
	{
		// Empty
	}

	public Instance pipe(Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		for (int i = 0; i < ts.size(); i++)
		{
			Token t = ts.get(i);
			String text = t.getText();
			if (brief)
			{
				text = text.replaceAll("[A-Z]+", "A");
				text = text.replaceAll("[a-z]+", "a");
				text = text.replaceAll("[0-9]+", "0");
				text = text.replaceAll("[^A-Za-z0-9]+", "x");
			}
			else
			{
				text = text.replaceAll("[A-Z]", "A");
				text = text.replaceAll("[a-z]", "a");
				text = text.replaceAll("[0-9]", "0");
				text = text.replaceAll("[^A-Za-z0-9]", "x");
			}
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
		out.writeBoolean(brief);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		prefix = (String) in.readObject();
		matchingRegex = (Pattern) in.readObject();
		brief = in.readBoolean();
	}
}
