package io.dico.dicore.command.chat.help;

public interface IHelpComponent {

    int lineCount();

    void appendTo(StringBuilder sb);

}
