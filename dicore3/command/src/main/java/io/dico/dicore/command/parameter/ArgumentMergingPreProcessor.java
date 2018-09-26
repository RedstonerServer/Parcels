package io.dico.dicore.command.parameter;

public class ArgumentMergingPreProcessor implements IArgumentPreProcessor {
    private final String tokens;
    private final char escapeChar;

    public ArgumentMergingPreProcessor(String tokens, char escapeChar) {
        if ((tokens.length() & 1) != 0 || tokens.isEmpty()) throw new IllegalArgumentException();
        this.tokens = tokens;
        this.escapeChar = escapeChar;
    }

    @Override
    public String[] process(int argStart, String[] args) {
        if (!(0 <= argStart && argStart <= args.length)) {
            throw new IndexOutOfBoundsException();
        }

        Parser parser = new Parser(argStart, args.clone());
        return parser.doProcess();
    }

    private class Parser {
        private final int argStart;
        private final String[] args;

        private int currentIndex;
        private int sectionStart;
        private char closingToken;
        private int sectionEnd;
        private int removeCount;

        Parser(int argStart, String[] args) {
            this.argStart = argStart;
            this.args = args;
        }

        private void reset() {
            removeCount = 0;
            closingToken = 0;
            sectionStart = -1;
            sectionEnd = -1;
            currentIndex = argStart;
        }

        private boolean findNextSectionStart() {
            while (currentIndex < args.length) {
                String arg = args[currentIndex];
                if (arg == null) {
                    throw new IllegalArgumentException();
                }

                if (arg.isEmpty()) {
                    ++currentIndex;
                    continue;
                }

                int openingTokenIndex = tokens.indexOf(arg.charAt(0));
                if (openingTokenIndex == -1 || (openingTokenIndex & 1) != 0) {
                    ++currentIndex;
                    continue;
                }

                // found
                closingToken = tokens.charAt(openingTokenIndex | 1);
                sectionStart = currentIndex;
                return true;
            }

            return false;
        }

        private boolean findNextSectionEnd() {
            while (currentIndex < args.length) {
                String arg = args[currentIndex];
                if (arg == null) {
                    throw new IllegalArgumentException();
                }

                if (arg.isEmpty()
                    || arg.charAt(arg.length() - 1) != closingToken
                    || (sectionStart == currentIndex && arg.length() == 1)) {
                    ++currentIndex;
                    continue;
                }

                if (escapeChar != 0
                    && arg.length() > 1
                    && arg.charAt(arg.length() - 2) == escapeChar) {
                    // escaped
                    ++currentIndex;
                    continue;
                }

                // found
                closingToken = 0;
                sectionEnd = currentIndex;
                ++currentIndex;
                return true;
            }

            return false;
        }

        private void processFoundSection() {
            if (sectionStart == sectionEnd) {
                String arg = args[sectionStart];
                args[sectionStart] = arg.substring(1, arg.length() - 1);
                return;
            }

            removeCount += sectionEnd - sectionStart;

            StringBuilder sb = new StringBuilder();
            sb.append(args[sectionStart].substring(1));

            for (int i = sectionStart + 1; i < sectionEnd; i++) {
                sb.append(' ');
                sb.append(args[i]);
                args[i] = null;
            }
            sb.append(' ');
            sb.append(args[sectionEnd].substring(0, args[sectionEnd].length() - 1));
            args[sectionEnd] = null;

            args[sectionStart] = sb.toString();

            sectionStart = -1;
            sectionEnd = -1;
        }

        public String[] doProcess() {
            reset();

            while (findNextSectionStart()) {
                if (findNextSectionEnd()) {
                    processFoundSection();
                } else {
                    currentIndex = sectionStart + 1;
                }
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
        }

    }

}


