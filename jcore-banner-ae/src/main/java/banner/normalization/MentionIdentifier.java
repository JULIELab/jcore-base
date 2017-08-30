package banner.normalization;

import banner.types.Entity;
import banner.types.EntityIdentification;
import banner.types.Mention;
import banner.types.Sentence;

/**
 * Instances of this class are responsible for determining a set of candidate
 * {@link Entity} objects for each {@link Mention}. This is done by adding an
 * instance of {@link EntityIdentification} to the {@link Sentence} object.
 * 
 * @author bob
 * 
 */
public interface MentionIdentifier
{
	public void addEntity(Entity entity);

	public void identifyMentions(Sentence sentence);

}
