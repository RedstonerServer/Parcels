package io.dico.dicore.command.chat;

import io.dico.dicore.command.Command;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.ParameterList;

import java.util.*;
import java.util.stream.Collectors;

public class HelpCache {
    private static Map<ICommandAddress, HelpCache> caches = new IdentityHashMap<>();
    private ICommandAddress address;
    private String shortSyntax;
    private String[] lines;
    private int[] pageStarts;

    public static HelpCache getHelpCache(ICommandAddress address) {
        return caches.computeIfAbsent(address, HelpCache::new);
    }

    private HelpCache(ICommandAddress address) {
        this.address = address;
    }

    private void loadHelp() {
        List<String> lines = new ArrayList<>();
        List<Integer> potentialPageStarts = new ArrayList<>();
        int curLineIdx = 0;
        potentialPageStarts.add(curLineIdx);

        String curLine = address.getChatController().getMessagePrefixForType(EMessageType.INSTRUCTION);
        curLine += address.getChatController().getChatFormatForType(EMessageType.INSTRUCTION);
        curLine += getSyntax();
        lines.add(curLine);
        curLineIdx++;

        if (address.hasCommand()) {
            Command command = address.getCommand();
            String[] description = command.getDescription();
            if (description != null && description.length > 0) {
                for (String line : description) {
                    curLine = address.getChatController().getChatFormatForType(EMessageType.INFORMATIVE).toString();
                    curLine += line;
                    lines.add(curLine);
                    curLineIdx++;
                }
            }
        }

        List<ICommandAddress> children = address.getChildren().values().stream()
                .distinct()
                .sorted(Comparator.comparing(ICommandAddress::getMainKey))
                .collect(Collectors.toList());

        for (ICommandAddress address : children) {
            potentialPageStarts.add(curLineIdx);

            curLine = this.address.getChatController().getChatFormatForType(EMessageType.INSTRUCTION) + "/";
            if (address.isDepthLargerThan(2)) {
                curLine += "... ";
            }
            curLine += address.getMainKey();
            curLine += getHelpCache(address).getShortSyntax();
            lines.add(curLine);
            curLineIdx++;

            if (address.hasCommand()) {
                String shortDescription = address.getCommand().getShortDescription();
                if (shortDescription != null) {
                    curLine = this.address.getChatController().getChatFormatForType(EMessageType.INFORMATIVE).toString();
                    curLine += shortDescription;
                    lines.add(curLine);
                    curLineIdx++;
                }
            }
        }

        this.lines = lines.toArray(new String[lines.size()]);

        // compute where the pages start with a maximum page size of 10
        List<Integer> pageStarts = new ArrayList<>();
        pageStarts.add(0);
        int maxLength = 10;
        int curPageEndTarget = maxLength;
        for (int i = 1, n = potentialPageStarts.size(); i < n; i++) {
            int index = potentialPageStarts.get(i);
            if (index == curPageEndTarget) {
                pageStarts.add(curPageEndTarget);
                curPageEndTarget += maxLength;
            } else if (index > curPageEndTarget) {
                curPageEndTarget = potentialPageStarts.get(i - 1);
                pageStarts.add(curPageEndTarget);
                curPageEndTarget += maxLength;
            }
        }

        int[] pageStartsArray = new int[pageStarts.size()];
        for (int i = 0, n = pageStartsArray.length; i < n; i++) {
            pageStartsArray[i] = pageStarts.get(i);
        }
        this.pageStarts = pageStartsArray;
    }

    /**
     * Get a help page
     *
     * @param page the 0-bound page number (first page is page 0)
     * @return the help page
     */
    public String getHelpPage(int page) {
        if (lines == null) {
            loadHelp();
        }

        //System.out.println(Arrays.toString(lines));

        if (page >= pageStarts.length) {
            //System.out.println("page >= pageStarts.length: " + Arrays.toString(pageStarts));
            return "";
        } else if (page < 0) {
            throw new IllegalArgumentException("Page number is negative");
        }

        int start = pageStarts[page];
        int end = page + 1 == pageStarts.length ? lines.length : pageStarts[page + 1];
        //System.out.println("start = " + start);
        //System.out.println("end = " + end);
        return String.join("\n", Arrays.copyOfRange(lines, start, end));
    }

    public int getTotalPageCount() {
        return pageStarts.length;
    }

    /**
     * The latter syntax of the command, prefixed by a space.
     *
     * @return The latter part of the syntax for this command. That is, without the actual command name.
     */
    public String getShortSyntax() {
        if (shortSyntax != null) {
            return shortSyntax;
        }

        StringBuilder syntax = new StringBuilder();
        if (address.hasCommand()) {
            Command command = address.getCommand();
            ParameterList list = command.getParameterList();
            Parameter<?, ?> repeated = list.getRepeatedParameter();

            int requiredCount = list.getRequiredCount();
            List<Parameter<?, ?>> indexedParameters = list.getIndexedParameters();
            for (int i = 0, n = indexedParameters.size(); i < n; i++) {
                syntax.append(i < requiredCount ? " <" : " [");
                Parameter<?, ?> param = indexedParameters.get(i);
                syntax.append(param.getName());
                if (param == repeated) {
                    syntax.append("...");
                }
                syntax.append(i < requiredCount ? '>' : ']');
            }

            Map<String, Parameter<?, ?>> parametersByName = list.getParametersByName();
            for (Parameter<?, ?> param : parametersByName.values()) {
                if (param.isFlag()) {
                    syntax.append(" [").append(param.getName());
                    if (param.expectsInput()) {
                        syntax.append(" <>");
                    }
                    syntax.append(']');
                }
            }
        } else {
            syntax.append(' ');
        }
        this.shortSyntax = syntax.toString();
        return this.shortSyntax;
    }

    public String getSyntax() {
        return '/' + address.getAddress() + getShortSyntax();
    }

}
