/*
 * Copyright (c) 2017 ProjectOreville
 *
 * All rights reserved.
 *
 * Author(s):
 *   Dico Karssiens
 */

package io.dico.dicore;

import org.jetbrains.annotations.NotNull;

public final class Formatting implements CharSequence {
    public static final char FORMAT_CHAR = '\u00a7';
    private static final String CACHED_CHARS = "0123456789abcdefklmnor";
    private static final Formatting[] singleCharInstances = new Formatting[CACHED_CHARS.length()];

    @NotNull
    public static final Formatting
            BLACK = from('0'),
            DARK_BLUE = from('1'),
            DARL_GREEN = from('2'),
            CYAN = from('3'),
            DARK_RED = from('4'),
            PURPLE = from('5'),
            ORANGE = from('6'),
            GRAY = from('7'),
            DARK_GRAY = from('8'),
            BLUE = from('9'),
            GREEN = from('a'),
            AQUA = from('b'),
            RED = from('c'),
            PINK = from('d'),
            YELLOW = from('e'),
            WHITE = from('f'),
            BOLD = from('l'),
            STRIKETHROUGH = from('m'),
            UNDERLINE = from('n'),
            ITALIC = from('o'),
            MAGIC = from('k'),
            RESET = from('r'),
            EMPTY = from('\0');
    
    public static String stripAll(String value) {
        return stripAll(FORMAT_CHAR, value);
    }
    
    public static String stripAll(char alternateChar, String value) {
        int index = value.indexOf(alternateChar);
        int max;
        if (index == -1 || index == (max = value.length() - 1)) {
            return value;
        }
        
        StringBuilder result = new StringBuilder();
        int from = 0;
        do {
            if (isRecognizedChar(value.charAt(index + 1))) {
                result.append(value, from, index);
                from = index + 2;
            } else {
                result.append(value, from, from = index + 2);
            }
            
            index = value.indexOf(alternateChar, index + 1);
        } while (index != -1 && index != max && from <= max);
        
        if (from <= max) {
            result.append(value, from, value.length());
        }
        return result.toString();
    }
    
    public static String stripFirst(String value) {
        return stripFirst(FORMAT_CHAR, value);
    }
    
    public static String stripFirst(char alternateChar, String value) {
        int index = value.indexOf(alternateChar);
        int max;
        if (index == -1 || index == (max = value.length() - 1)) {
            return value;
        }
        
        StringBuilder result = new StringBuilder(value.length());
        int from = 0;
        if (isRecognizedChar(value.charAt(index + 1))) {
            result.append(value, from, index);
            from = index + 2;
        } else {
            result.append(value, from, from = index + 2);
        }
        
        if (from < max) {
            result.append(value, from, value.length());
        }
        return result.toString();
    }
    
    public static Formatting from(char c) {
        if (isRecognizedChar(c)) {
            c = Character.toLowerCase(c);
            int index = CACHED_CHARS.indexOf(c);
            if (index == -1) return EMPTY;

            Formatting res = singleCharInstances[index];
            if (res == null) {
                singleCharInstances[index] = res = new Formatting(c);
            }
            return res;
        }
        return EMPTY;
    }
    
    public static Formatting from(String chars) {
        return chars.length() == 1 ? from(chars.charAt(0)) : getFormats(chars, '\0');
    }
    
    public static Formatting getFormats(String input) {
        return getFormats(input, FORMAT_CHAR);
    }
    
    public static Formatting getFormats(String input, char formatChar) {
        return getFormats(input, 0, input.length(), formatChar);
    }
    
    public static Formatting getFormats(String input, int start, int end, char formatChar) {
        if ((start < 0) || (start > end) || (end > input.length())) {
            throw new IndexOutOfBoundsException("start " + start + ", end " + end + ", input.length() " + input.length());
        }
        
        boolean needsFormatChar = formatChar != '\0';
        char[] formats = new char[6];
        // just make sure it's not the same as formatChar
        char previous = (char) (formatChar + 1);
        
        for (int i = start; i < end; i++) {
            char c = input.charAt(i);
            
            if (previous == formatChar || !needsFormatChar) {
                if (isColourChar(c) || isResetChar(c)) {
                    formats = new char[6];
                    formats[0] = Character.toLowerCase(c);
                } else if (isFormatChar(c)) {
                    char format = Character.toLowerCase(c);
                    for (int j = 0; j < 6; j++) {
                        if (formats[j] == '\0') {
                            formats[j] = format;
                            break;
                        } else if (formats[j] == format) {
                            break;
                        }
                    }
                }
            }
            
            previous = c;
        }
        
        return formats[1] == '\0' ? from(formats[0]) : new Formatting(formats);
    }
    
    public static String translate(String input) {
        return translateChars('&', input);
    }
    
    public static String translateChars(char alternateChar, String input) {
        return translateFormat(alternateChar, FORMAT_CHAR, input);
    }
    
    public static String revert(String input) {
        return revertChars('&', input);
    }
    
    public static String revertChars(char alternateChar, String input) {
        return translateFormat(FORMAT_CHAR, alternateChar, input);
    }
    
    public static String translateFormat(char fromChar, char toChar, String input) {
        if (input == null) {
            return null;
        }
        int n = input.length();
        if (n < 2) {
            return input;
        }
        char[] result = null;
        char previous = input.charAt(0);
        for (int i = 1; i < n; i++) {
            char c = input.charAt(i);
            if (previous == fromChar && isRecognizedChar(c)) {
                if (result == null) {
                    result = input.toCharArray();
                }
                result[i - 1] = toChar;
            }
            previous = c;
        }
        return result == null ? input : String.valueOf(result);
    }
    
    public static void translate(StringBuilder input) {
        translateChars('&', input);
    }
    
    public static void translateChars(char alternateChar, StringBuilder input) {
        translateFormat(alternateChar, FORMAT_CHAR, input);
    }
    
    public static void revert(StringBuilder input) {
        revertChars('&', input);
    }
    
    public static void revertChars(char alternateChar, StringBuilder input) {
        translateFormat(FORMAT_CHAR, alternateChar, input);
    }
    
    public static void translateFormat(char fromChar, char toChar, StringBuilder input) {
        if (input == null) {
            return;
        }
        int n = input.length();
        if (n < 2) {
            return;
        }
        char previous = input.charAt(0);
        for (int i = 1; i < n; i++) {
            char c = input.charAt(i);
            if (previous == fromChar && isRecognizedChar(c)) {
                input.setCharAt(i -1, toChar);
            }
            previous = c;
        }
    }

    private static boolean isRecognizedChar(char c) {
        return isColourChar(c) || isFormatChar(c) || isResetChar(c);
    }
    
    private static boolean isColourChar(char c) {
        return "0123456789abcdefABCDEF".indexOf(c) >= 0;
    }
    
    private static boolean isResetChar(char c) {
        return c == 'r' || c == 'R';
    }
    
    private static boolean isFormatChar(char c) {
        return "klmnoKLMNO".indexOf(c) >= 0;
    }
    
    private final String format;
    
    private Formatting(char[] formats) {
        StringBuilder format = new StringBuilder(12);
        for (char c : formats) {
            if (c != '\0') {
                format.append(FORMAT_CHAR).append(c);
            } else {
                break;
            }
        }
        this.format = format.toString();
    }
    
    private Formatting(char c) {
        this.format = (c != '\0') ? String.valueOf(new char[]{FORMAT_CHAR, c}) : "";
    }
    
    @Override
    public int length() {
        return format.length();
    }
    
    @Override
    public char charAt(int index) {
        return format.charAt(index);
    }
    
    @Override
    public String subSequence(int start, int end) {
        return format.substring(start, end);
    }
    
    @Override
    public String toString() {
        return format;
    }
    
    public String toString(char formatChar) {
        return format.replace(FORMAT_CHAR, formatChar);
    }
    
}