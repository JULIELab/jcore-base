package banner.postprocessing;

import banner.types.Mention;
import banner.types.Sentence;

/**
 * Instances of {@link PostProcessor} take {@link Sentence}s which have been tagged and modify the set of
 * {@link Mention}s according to some criteria.
 * 
 * @author Bob
 */
public interface PostProcessor
{
	// TODO Collapse all forms of post-processing into a single class

	/**
	 * @param sentence
	 *            The {@link Sentence} to perform post-processing on
	 */
	public void postProcess(Sentence sentence);

}