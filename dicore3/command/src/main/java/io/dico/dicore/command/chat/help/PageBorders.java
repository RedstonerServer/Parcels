package io.dico.dicore.command.chat.help;

import java.util.Arrays;

public class PageBorders {
    private final IPageBorder header, footer;

    public PageBorders(IPageBorder header, IPageBorder footer) {
        this.header = header;
        this.footer = footer;
    }

    public IPageBorder getHeader() {
        return header;
    }

    public IPageBorder getFooter() {
        return footer;
    }

    public static IPageBorder simpleBorder(String... lines) {
        return new SimplePageBorder(lines);
    }

    public static IPageBorder disappearingBorder(int pageNum, String... lines) {
        return disappearingBorder(pageNum, 0, lines);
    }

    public static IPageBorder disappearingBorder(int pageNum, int keptLines, String... lines) {
        return new DisappearingPageBorder(pageNum, keptLines, lines);
    }

    static class SimplePageBorder extends SimpleHelpComponent implements IPageBorder {
        private final String replacedSequence;

        public SimplePageBorder(String replacedSequence, String... lines) {
            super(lines);
            this.replacedSequence = replacedSequence;
        }

        public SimplePageBorder(String... lines) {
            super(lines);
            this.replacedSequence = "%pageCount%";
        }

        @Override
        public void setPageCount(int pageCount) {
            String[] lines = this.lines;
            for (int i = 0; i < lines.length; i++) {
                lines[i] = lines[i].replace(replacedSequence, Integer.toString(pageCount));
            }
        }

    }

    static class DisappearingPageBorder extends SimpleHelpComponent implements IPageBorder {
        private final int pageNum;
        private final int keptLines;

        public DisappearingPageBorder(int pageNum, int keptLines, String... lines) {
            super(lines);
            this.pageNum = pageNum;
            this.keptLines = keptLines;
        }

        @Override
        public void setPageCount(int pageCount) {
            if (pageCount == pageNum) {
                String[] lines = this.lines;
                this.lines = Arrays.copyOfRange(lines, Math.max(0, lines.length - keptLines), lines.length);
            }
        }

    }

}
