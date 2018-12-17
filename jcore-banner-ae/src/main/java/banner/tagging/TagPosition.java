/**
 * 
 */
package banner.tagging;

import banner.types.Mention;

public enum TagPosition
{
	I, O, B, E, W;

	public TagPosition convert(TagFormat format)
	{
		if (this == I || this == O)
			return this;
		if (format == TagFormat.IO)
			return I;
		if (format == TagFormat.IOB)
			if (this == E)
				return I;
			else
				return B;
		if (format == TagFormat.IOE)
			if (this == B)
				return I;
			else
				return E;
		return this;
	}

	public static TagPosition getPostion(Mention mention, int index)
	{
		if (mention == null)
			return O;
		if (index < mention.getStart() || index >= mention.getEnd())
			throw new IllegalArgumentException();
		if (mention.length() == 1)
			return W;
		if (index == mention.getStart())
			return B;
		if (index == mention.getEnd() - 1)
			return E;
		return I;
	}

	public static String getPositionText(TagFormat format, Mention mention, int index)
	{
		return TagPosition.getPostion(mention, index).convert(format).name() + "-" + mention.getEntityType().getText();
	}
}