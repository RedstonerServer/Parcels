package io.dico.dicore.command.parameter;

/**
 * An interface to process tokens such as quotes
 */
@Deprecated
public interface IArgumentPreProcessor {

    /**
     * Preprocess the arguments contained within the given ArgumentBuffer.
     * If no changes are made, this might return the same buffer.
     * Any arguments preceding {@code buffer.getCursor()} will not be affected.
     *
     * <p>
     *     If {@code count} is non-negative, it declares a limit on the number of arguments after preprocessing.
     *     In that case, the buffer's cursor is set to the index of the first argument following processed arguments.
     * </p>
     *
     * @param buffer  the argument buffer
     * @param count the maximum number of (processed) arguments
     * @return the arguments after preprocessing
     */
    ArgumentBuffer process(ArgumentBuffer buffer, int count);

    IArgumentPreProcessor NONE = (buffer, count) -> buffer;

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

