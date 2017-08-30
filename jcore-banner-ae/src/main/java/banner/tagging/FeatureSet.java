package banner.tagging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import banner.tagging.pipe.LChar;
import banner.tagging.pipe.LemmaPOS;
import banner.tagging.pipe.LowerCaseTokenText;
import banner.tagging.pipe.Pretagger;
import banner.tagging.pipe.RChar;
import banner.tagging.pipe.Sentence2TokenSequence;
import banner.tagging.pipe.SimFind;
import banner.tagging.pipe.TokenNumberClass;
import banner.tagging.pipe.TokenWordClass;
import banner.types.Mention.MentionType;
import banner.types.Sentence.OverlapOption;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.OffsetConjunctions;
import cc.mallet.pipe.tsf.RegexMatches;
import cc.mallet.pipe.tsf.TokenTextCharNGrams;
import cc.mallet.pipe.tsf.TokenTextCharPrefix;
import cc.mallet.pipe.tsf.TokenTextCharSuffix;
import dragon.nlp.tool.Lemmatiser;

public class FeatureSet implements Serializable
{

	// TODO Can / should this be expanded into a general configuration class?

	private static final long serialVersionUID = -4591127831978244954L;

	private static String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";

	private SerialPipes pipe;

	public FeatureSet(TagFormat format, Lemmatiser lemmatiser, dragon.nlp.tool.Tagger posTagger, banner.tagging.Tagger preTagger, String simFindFilename, Set<MentionType> mentionTypes, OverlapOption sameType, OverlapOption differentType)
	{
		pipe = createPipe(format, lemmatiser, posTagger, preTagger, simFindFilename, mentionTypes, sameType, differentType);
	}

	public void setLemmatiser(Lemmatiser lemmatiser)
	{
		((LemmaPOS) pipe.getPipe(1)).setLemmatiser(lemmatiser);
	}

	public void setPosTagger(dragon.nlp.tool.Tagger posTagger)
	{
		((LemmaPOS) pipe.getPipe(1)).setPosTagger(posTagger);
	}

	public void setPreTagger(banner.tagging.Tagger preTagger)
	{
		((Pretagger) pipe.getPipe(2)).setPreTagger(preTagger);
	}

	public Pipe getPipe()
	{
		return pipe;
	}

	private SerialPipes createPipe(TagFormat format, Lemmatiser lemmatiser, dragon.nlp.tool.Tagger posTagger, banner.tagging.Tagger preTagger, String simFindFilename, Set<MentionType> mentionTypes,
			OverlapOption sameType, OverlapOption differentType)
	{
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		// TODO Test feature variations
		// TODO Make configurable which features to use
		// TODO Try dropping redundant features
		pipes.add(new Sentence2TokenSequence(format, mentionTypes, sameType, differentType));
		pipes.add((lemmatiser == null && posTagger == null) ? new Noop() : new LemmaPOS(lemmatiser, posTagger));
		pipes.add((preTagger == null) ? new Noop() : new Pretagger("PRETAG=", preTagger));
		pipes.add(new LChar("LCHAR="));
		pipes.add(new RChar("RCHAR="));

		// pipes.add(new TokenText("TT="));
		pipes.add(new LowerCaseTokenText("W="));
		// pipes.add(new TokenLength("WLEN="));
		pipes.add(new TokenNumberClass("NC=", false));
		pipes.add(new TokenNumberClass("BNC=", true));
		pipes.add(new TokenWordClass("WC=", false));
		pipes.add(new TokenWordClass("BWC=", true));
		pipes.add(new RegexMatches("ALPHA", Pattern.compile("[A-Za-z]+")));
		pipes.add(new RegexMatches("INITCAPS", Pattern.compile("[A-Z].*")));
		pipes.add(new RegexMatches("UPPER-LOWER", Pattern.compile("[A-Z][a-z].*")));
		pipes.add(new RegexMatches("LOWER-UPPER", Pattern.compile("[a-z]+[A-Z]+.*")));
		pipes.add(new RegexMatches("ALLCAPS", Pattern.compile("[A-Z]+")));
		pipes.add(new RegexMatches("MIXEDCAPS", Pattern.compile("[A-Z][a-z]+[A-Z][A-Za-z]*")));
		pipes.add(new RegexMatches("SINGLECHAR", Pattern.compile("[A-Za-z]")));
		pipes.add(new RegexMatches("SINGLEDIGIT", Pattern.compile("[0-9]")));
		pipes.add(new RegexMatches("DOUBLEDIGIT", Pattern.compile("[0-9][0-9]")));
		pipes.add(new RegexMatches("NUMBER", Pattern.compile("[0-9,]+")));
		pipes.add(new RegexMatches("HASDIGIT", Pattern.compile(".*[0-9].*")));
		pipes.add(new RegexMatches("ALPHANUMERIC", Pattern.compile(".*[0-9].*[A-Za-z].*")));
		pipes.add(new RegexMatches("ALPHANUMERIC", Pattern.compile(".*[A-Za-z].*[0-9].*")));
		pipes.add(new RegexMatches("NUMBERS_LETTERS", Pattern.compile("[0-9]+[A-Za-z]+")));
		pipes.add(new RegexMatches("LETTERS_NUMBERS", Pattern.compile("[A-Za-z]+[0-9]+")));

		// TODO Change these to multi-token features
		pipes.add(new RegexMatches("HAS_DASH", Pattern.compile(".*-.*")));
		pipes.add(new RegexMatches("HAS_QUOTE", Pattern.compile(".*'.*")));
		pipes.add(new RegexMatches("HAS_SLASH", Pattern.compile(".*/.*")));
		pipes.add(new RegexMatches("REALNUMBER", Pattern.compile("(-|\\+)?[0-9,]+(\\.[0-9]*)?%?")));
		pipes.add(new RegexMatches("REALNUMBER", Pattern.compile("(-|\\+)?[0-9,]*(\\.[0-9]+)?%?")));
		pipes.add(new RegexMatches("START_MINUS", Pattern.compile("-.*")));
		pipes.add(new RegexMatches("START_PLUS", Pattern.compile("\\+.*")));
		pipes.add(new RegexMatches("END_PERCENT", Pattern.compile(".*%")));

		pipes.add(new TokenTextCharPrefix("2PREFIX=", 2));
		pipes.add(new TokenTextCharPrefix("3PREFIX=", 3));
		pipes.add(new TokenTextCharPrefix("4PREFIX=", 4));
		pipes.add(new TokenTextCharSuffix("2SUFFIX=", 2));
		pipes.add(new TokenTextCharSuffix("3SUFFIX=", 3));
		pipes.add(new TokenTextCharSuffix("4SUFFIX=", 4));
		pipes.add(new TokenTextCharNGrams("CHARNGRAM=", new int[] { 2, 3 }, true));
		pipes.add(new RegexMatches("ROMAN", Pattern.compile("[IVXDLCM]+", Pattern.CASE_INSENSITIVE)));
		pipes.add(new RegexMatches("GREEK", Pattern.compile(GREEK, Pattern.CASE_INSENSITIVE)));
		// TODO try breaking this into several sets (brackets, sentence marks, etc.)
		pipes.add(new RegexMatches("ISPUNCT", Pattern.compile("[`~!@#$%^&*()-=_+\\[\\]\\\\{}|;\':\\\",./<>?]+")));
		//siddhartha added these;
		pipes.add(simFindFilename == null ? new Noop() : new SimFind(simFindFilename));
		pipes.add(new OffsetConjunctions(new int[][] { { -2 }, { -1 }, { 1 }, { 2 } }));
		pipes.add(new TokenSequence2FeatureVectorSequence(true, true));
		return new SerialPipes(pipes);
	}
}
