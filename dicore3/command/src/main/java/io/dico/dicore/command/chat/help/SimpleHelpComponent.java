package io.dico.dicore.command.chat.help;

public class SimpleHelpComponent implements IHelpComponent {
    String[] lines;

    public SimpleHelpComponent(String... lines) {
        this.lines = lines;
    }

    @Override
    public int lineCount() {
        return lines.length;
    }

    @Override
    public void appendTo(StringBuilder sb) {
        String[] lines = this.lines;
        int len = lines.length;
        if (0 < len) {
            sb.append(lines[0]);
        }
        for (int i = 1; i < len; i++) {
            sb.append('\n').append(lines[i]);
        }
    }

}
