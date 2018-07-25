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
        if (tokens.isEmpty() || (tokens.length() & 1) != 0) {
            throw new IllegalArgumentException();
        }

        return (argStart, args) -> {
            if (!(0 <= argStart && argStart <= args.length)) {
                throw new IndexOutOfBoundsException();
            }

            args = args.clone();
            int removeCount = 0;
            int closingTokenIdx = 0;
            int sectionStart = -1;

            for (int i = argStart; i < args.length; i++) {
                String arg = args[i];
                if (arg == null || arg.isEmpty()) {
                    continue;
                }

                if (closingTokenIdx != 0) {
                    int idx = tokens.indexOf(arg.charAt(arg.length() - 1));
                    if (idx == closingTokenIdx) {

                        // count escape chars
                        int index = arg.length() - 1;
                        int count = 0;
                        while (index > 0 && arg.charAt(--index) == escapeChar) {
                            count++;
                        }

                        // remove the final char plus half the count, rounding upwards.
                        args[i] = arg.substring(0, args.length - 1 - (count + 1) / 2);

                        if ((count & 1) == 0) {
                            // not escaped
                            StringBuilder concat = new StringBuilder(args[sectionStart].substring(1));
                            for (int j = sectionStart + 1; j <= i; j++) {
                                concat.append(' ').append(args[j]);
                                args[j] = null;
                                removeCount++;
                            }

                            args[sectionStart] = concat.toString();

                            sectionStart = -1;
                            closingTokenIdx = 0;

                        } else {
                            // it's escaped
                            // add final char because it was escaped
                            args[i] += tokens.charAt(closingTokenIdx);

                        }
                    }

                    if (i == args.length - 1) {
                        // if the closing token isn't found, reset state and start from the index subsequent to the one where the opener was found
                        // it should also undo removal of any escapes... it doesn't do that
                        i = sectionStart + 1;
                        closingTokenIdx = 0;
                        sectionStart = -1;
                    }

                    continue;
                }

                int idx = tokens.indexOf(arg.charAt(0));
                if (idx == -1 || (idx & 1) != 0) {
                    continue;
                }

                closingTokenIdx = idx | 1;
                sectionStart = i;

                // make sure to check from the current index for a closer
                i--;
            }

            if (removeCount == 0) {
                return args;
            }

            String[] result = new String[args.length - removeCount];
            int i = 0;
            for (String arg : args) {
                if (arg != null) {
                    result[i++] = arg;
                }
            }

            return result;
        };

    }

}
