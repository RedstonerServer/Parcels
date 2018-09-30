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
    public ArgumentBuffer process(ArgumentBuffer buffer, int count) {
        Parser parser = new Parser(buffer.getArray().clone(), buffer.getCursor(), count);
        String[] array = parser.doProcess();
        ArgumentBuffer result = new ArgumentBuffer(array);
        parser.updateBuffer(result);
        return result;
    }

    private class Parser {
        private final String[] args;
        private final int start;
        private final int count;

        private int foundSectionCount;
        private int currentIndex;
        private int sectionStart;
        private char closingToken;
        private int sectionEnd;
        private int removeCount;

        Parser(String[] args, int start, int count) {
            this.start = start;
            this.args = args;
            this.count = count;
        }

        private void reset() {
            foundSectionCount = 0;
            currentIndex = start;
            sectionStart = -1;
            closingToken = 0;
            sectionEnd = -1;
            removeCount = 0;
        }

        private boolean findNextSectionStart() {
            if (count >= 0 && foundSectionCount >= count) return false;

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

            ++foundSectionCount;
        }

        String[] doProcess() {
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

        void updateBuffer(ArgumentBuffer buffer) {
            if (count < 0) {
                buffer.setCursor(start);
            } else {
                buffer.setCursor(currentIndex);
            }
        }

    }

}


