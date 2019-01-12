package lingscope.drivers;

import lingscope.algorithms.Annotator;

/**
 * Driver to train a model file. The training data will have to be provided.
 * @author shashank
 */
public class ModelTrainer {

    /**
     * Prints the usage for the model trainer
     */
    public static void usage() {
        System.out.println("Usage:\njava lingscope.drivers.ModelTrainer (cue|scope) (crf|baseline|negex) training_data_file file_where_model_will_be_saved");
    }

    /**
     *
     * @param args
     * 0 - Annotator type ("cue" or "scope")
     * 1 - Model type ("crf", "baseline" or "negex")
     * 2 - File from which training data will be read
     * 2 - File where the model will be saved
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            usage();
            System.exit(0);
        }
        Annotator annotator = SentenceTagger.getAnnotator(args[1], args[0]);
        if (annotator == null) {
            usage();
            System.exit(1);
        }
        annotator.serializeAnnotator(args[2], args[3]);
    }
    
}
