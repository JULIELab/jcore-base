package banner.tagging.pipe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * Taken from GNormPlus code: Wei, C. H., Kao, H. Y., & Lu, Z. (2015).
 * GNormPlus: An Integrative Approach for Tagging Genes, Gene Families, and
 * Protein Domains. BioMed Research International, 2015.
 * https://doi.org/10.1155/2015/918710
 * 
 * @author faessler
 *
 */
public class MentionTypeHint extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = -163529848335469810L;
	private String prefix;

	public MentionTypeHint(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public Instance pipe(Instance inst) {
		TokenSequence ts = (TokenSequence) inst.getData();
		String[] tokens = ts.stream().map(Token::getText).toArray(String[]::new);
		for (int p = 0; p < tokens.length; ++p) {
			String mentionType = null;
			if(tokens[p].matches("(ytochrome|cytochrome)")){mentionType="-Type_cytochrome-";}
			else if(tokens[p].matches(".*target") ){mentionType="-Type_target-";}
			else if(tokens[p].matches(".*(irradiation|hybrid|fusion|experiment|gst|est|gap|antigen)") ){mentionType="-Type_ExperimentNoun-";}
			else if(tokens[p].matches(".*(disease|disorder|dystrophy|deficiency|syndrome|dysgenesis|cancer|injury|neoplasm|diabetes|diabete)") ){mentionType="-Type_Disease-";}
			else if(tokens[p].matches(".*(motif|domain|omain|binding|site|region|sequence|frameshift|finger|box).*") ){mentionType="-Type_DomainMotif-";}
			else if(tokens[p].equals("-") && (p<tokens.length-1 && tokens[p+1].matches(".*(motif|domain|omain|binding|site|region|sequence|frameshift|finger|box).*")) ){mentionType="-Type_DomainMotif-";}
			else if(tokens[p].matches("[rmc]") && (p<tokens.length-1 && (tokens[p+1].equals("DNA") || tokens[p+1].equals("RNA")) ) ){mentionType="-Type_DomainMotif-";}
			else if(tokens[p].matches(".*(famil|complex|cluster|proteins|genes|factors|transporter|proteinase|membrane|ligand|enzyme|channels|tors$|ase$|ases$)") ){mentionType="-Type_Family-";}
			else if(tokens[p].toLowerCase().matches("^marker") ){mentionType="-Type_Marker-";}
			else if(tokens[p].equals(".*cell.*") || (p<tokens.length-1 && tokens[p+1].equals("cell") && tokens[p].matches("^(T|B|monocytic|cancer|tumor|myeloma|epithelial|crypt)$") ) ){mentionType="-Type_Cell-";}
			else if(tokens[p].equals(".*chromosome.*") ){mentionType="-Type_Chromosome-";}
			else if(tokens[p].matches("[pq]") && ( (p<tokens.length-1 && tokens[p+1].matches("^[0-9]+$")) || (p>0 && tokens[p-1].matches("^[0-9]+$")) ) ){mentionType="-Type_ChromosomeStrain-";}
			else if(tokens[p].matches(".*(related|regulated|associated|correlated|reactive).*")){mentionType="-Type_relation-";}
			else if(tokens[p].toLowerCase().matches(".*(polymorphism|mutation|deletion|insertion|duplication|genotype|genotypes).*") ){mentionType="-Type_VariationTerms-";}
			else if(tokens[p].matches(".*(oxidase|transferase|transferases|kinase|kinese|subunit|unit|receptor|adrenoceptor|transporter|regulator|transcription|antigen|protein|gene|factor|member|molecule|channel|deaminase|spectrin).*") ){mentionType="-Type_suffix-";}
			else if(tokens[p].matches("[\\(\\-\\_]") && (p<tokens.length-1 && tokens[p+1].toLowerCase().matches(".*(alpha|beta|gamma|delta|theta|kappa|zeta|sigma|omega|i|ii|iii|iv|v|vi|[abcdefgyr])")) ){mentionType="-Type_strain-";}
			else if(tokens[p].matches("(alpha|beta|gamma|delta|theta|kappa|zeta|sigma|omega|i|ii|iii|iv|v|vi|[abcdefgyr])") ){mentionType="-Type_strain-";}
			
			if (mentionType != null) {
				ts.get(p).setFeatureValue(prefix+mentionType, 1.0);
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
