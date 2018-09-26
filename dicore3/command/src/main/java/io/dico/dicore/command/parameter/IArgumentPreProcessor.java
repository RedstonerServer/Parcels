package io.dico.dicore.command.parameter;

/**
 * An interface to process tokens such as quotes
 */
public interface IArgumentPreProcessor {

    /**
     * Preprocess the arguments without modifying the array.
     * Might return the same array (in which case no changes were made).
     *
     * @param argStart the index within the array where the given arguments start (the part before that identifies the command)
     * @param args     the arguments
     * @return the arguments after preprocessing
     */
    String[] process(int argStart, String[] args);

    IArgumentPreProcessor NONE = (argStart, args) -> args;

    /**
     * Get an IArgumentPreProcessor that merges arguments between any two tokens
     *
     * @param tokens     The tokens that the merged arguments should be enclosed by, in subsequent pairs.
     *                   Example: []{}""
     *                   This would mean the following would be merged: [ hello this is a merged argument]
     * @param escapeChar the char that can be used to escape the given tokens
     * @return The IArgumentPreProcessor
     */
    static IArgumentPreProcessor mergeOnTokens(String tokens, char escapeChar) {
        return new ArgumentMergingPreProcessor(tokens, escapeChar);
    }

}
