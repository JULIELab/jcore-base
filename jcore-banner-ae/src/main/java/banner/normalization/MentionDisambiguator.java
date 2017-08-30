package banner.normalization;

import banner.types.Entity;
import banner.types.Mention;
import banner.types.Sentence;

/**
 * Instances of this class make the final determination of which {@link Entity}
 * a {@link Mention} refers. Entities must have been previously identified as
 * potential matches by an instance of {@link MentionIdentifier}.
 * 
 * @author bob
 */
public interface MentionDisambiguator
{
	public void disambiguateMentions(Sentence sentence);

}
