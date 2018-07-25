package io.dico.dicore;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class StringUtil {
    
    public static String capitalize(String input) {
        if (input.length() > 0) {
            char first = input.charAt(0);
            if (first != (first = Character.toUpperCase(first))) {
                char[] result = input.toCharArray();
                result[0] = first;
                return String.valueOf(result);
            }
        }
        return input;
    }
    
    /**
     * Capitalizes the first character of the string or the first character of each word
     *
     * @param input     the string to capitalize
     * @param spaceChar the character separating each word. If @code '\0' is passed, only the first character of
     *                  the input is capitalized.
     * @return the capitalized string
     */
    public static String capitalize(String input, char spaceChar) {
        if (spaceChar == '\0') {
            return capitalize(input);
        }
        
        char[] result = null;
        boolean capitalize = true;
        for (int n = input.length(), i = 0; i < n; i++) {
            char c = input.charAt(i);
            if (capitalize && c != (c = Character.toUpperCase(c))) {
                if (result == null) result = input.toCharArray();
                result[i] = c;
            }
            capitalize = c == spaceChar;
        }
        return result != null ? String.valueOf(result) : input;
    }
    
    public static String capitalize(String input, char spaceChar, char newSpaceChar) {
        if (newSpaceChar == '\0') {
            return capitalize(input, spaceChar);
        }
        
        char[] result = null;
        boolean capitalize = true;
        for (int n = input.length(), i = 0; i < n; i++) {
            char c = input.charAt(i);
            if (capitalize && c != (c = Character.toUpperCase(c))) {
                if (result == null) result = input.toCharArray();
                result[i] = c;
            }
            if (capitalize = c == spaceChar) {
                if (result == null) result = input.toCharArray();
                result[i] = newSpaceChar;
            }
        }
        return result != null ? String.valueOf(result) : input;
    }
    
    /**
     * Returns a lowercase version of the input with _ replaced with a space.
     * Mainly used for making names of enum constants readable.
     *
     * @param input
     * @return a humanified version of @code input
     */
    public static String humanify(String input) {
        return input == null ? null : input.toLowerCase().replace('_', ' ');
    }
    
    /**
     * Enumerate the given items, separating them by ", " and finally by " and "
     *
     * @param words the items to enumerate (it's not really enumerating....)
     * @return the enumerated string
     */
    public static String enumerate(String... words) {
        StringBuilder result = new StringBuilder();
        int size = words.length;
        int secondLastIndex = size - 2;
        for (int i = 0; i < size; i++) {
            String word = words[i];
            if (word.isEmpty())
                continue;
            result.append(word);
            if (i < secondLastIndex)
                result.append(", ");
            else if (i == secondLastIndex)
                result.append(" and ");
        }
        return result.toString();
    }
    
    public static String enumerate(String list, String regex) {
        return enumerate(list.split(regex));
    }
    
    /**
     * Return a formatted string of the length in millis, containing days, hours and minutes.
     *
     * @param length The delay in milliseconds
     * @return the formatted string
     */
    public static String getTimeLength(long length) {
        int minute = 60000; // in millis
        int hour = 60 * minute;
        int day = 24 * hour;
        
        int minutes = (int) ((length / minute) % 60);
        int hours = (int) ((length / hour) % 24);
        int days = (int) (length / day); //returns floor
        
        String result = ""; // It will be splitted at "|"
        if (days != 0)
            result += days + " days|";
        if (hours != 0)
            result += hours + " hours|";
        if (minutes != 0)
            result += minutes + " minutes|";
        return enumerate(result.split("\\|"));
    }
    
    /**
     * Return a formatted String to represent the given time length, in the given units
     *
     * @param sourceAmount   Amount of delay
     * @param sourceUnit     Unit of delay
     * @param ifEmpty        the String to return if the
     * @param displayedUnits units displayed
     * @return the formatted string
     * @throws IllegalArgumentException if there are no displayed units
     */
    public static String getTimeLength(long sourceAmount, TimeUnit sourceUnit, String ifEmpty, TimeUnit... displayedUnits) {
        if (displayedUnits.length == 0) {
            throw new IllegalArgumentException("No displayed units");
        }
        Arrays.sort(displayedUnits, Collections.reverseOrder(TimeUnit::compareTo)); // sort by opposite of enum declaration order (largest -> smallest)
        List<String> segments = new ArrayList<>(displayedUnits.length);
        for (TimeUnit unit : displayedUnits) {
            long displayedAmount = unit.convert(sourceAmount, sourceUnit);
            sourceAmount -= sourceUnit.convert(displayedAmount, unit);
            if (displayedAmount > 0) {
                String unitWord = unit.name().toLowerCase(); // plural
                if (displayedAmount == 1) {
                    unitWord = unitWord.substring(0, unitWord.length() - 1); // remove s at the end
                }
                segments.add(displayedAmount + " " + unitWord);
            }
        }
        return segments.isEmpty() ? ifEmpty : enumerate(segments.toArray(new String[segments.size()]));
    }
    
    /**
     * Returns the delay represented by a ban-like delay representation, in milliseconds
     * Example: "5d2h5m3s" for 5 days, 2 hours, 5 minutes and 3 seconds.
     * <p>
     * Supported characters are s, m, h, d, w.
     * Negative numbers are supported.
     *
     * @param input The input string
     * @return The delay in milliseconds
     * @throws IllegalArgumentException if the input string isn't properly formatted, or any non-digit character isn't recognized (capitals are not recognized).
     */
    public static long getTimeLength(String input) { //if -1: error
        long count = 0;
        int i = 0;
        while (i < input.length()) {
            int num = 0;
            char unit = '\0';
            boolean negate;
            if (negate = input.charAt(i) == '-') {
                i++;
            }
            do {
                char c = input.charAt(i);
                int digit = c - '0';
                if (0 <= digit && digit < 10) {
                    num = 10 * num + digit;
                } else {
                    unit = c;
                    break;
                }
            } while (i < input.length());
            
            long unitTime = getUnitTime(unit);
            if (unitTime == -1)
                throw new IllegalArgumentException();
            if (negate) {
                unitTime = -unitTime;
            }
            count += (num * unitTime);
        }
        return count;
    }
    
    /**
     * Returns the time represented by the given unit character in milliseconds.
     * <p>
     * 's' -> 1000
     * 'm' -> 1000 * 60
     * 'h' -> 1000 * 60 * 60
     * 'd' -> 1000 * 60 * 60 * 24
     * 'w' -> 1000 * 60 * 60 * 24 * 7
     * anything else -> -1
     *
     * @param unit The unit character, as shown above
     * @return the millisecond delay represented by the unit
     */
    public static long getUnitTime(char unit) { //if -1: no value found
        switch (Character.toLowerCase(unit)) {
            case 's':
                return 1000;
            case 'm':
                return 1000 * 60;
            case 'h':
                return 1000 * 60 * 60;
            case 'd':
                return 1000 * 60 * 60 * 24;
            case 'w':
                return 1000 * 60 * 60 * 24 * 7;
            default:
                return -1;
        }
    }
    
    /**
     * Computes a binary representation of the value.
     * The returned representation always displays 64 bits.
     * Every 8 bits, the digits are seperated by an _
     * The representation is prefixed by 0b.
     * <p>
     * Example: 0b00000000_11111111_00000001_11110000_00001111_11001100_00001111_10111010
     *
     * @param entry the value to represent in binary
     * @return A binary representation of the long value
     */
    public static String toBinaryString(long entry) {
        String binary = Long.toBinaryString(entry);
        String binary64 = String.valueOf(new char[64 - binary.length()]).replace('\0', '0') + binary;
        String withUnderscores = String.join("_", IntStream.range(0, 8).mapToObj(x -> binary64.substring(x * 8, x * 8 + 8)).toArray(String[]::new));
        return "0b" + withUnderscores;
    }
    
    /**
     * Turns a generic java classname into a name formatted properly to be an enum constant.
     *
     * @param name The string value I'd describe as a generic java classname (so we have CapitalCase)
     * @return An enum constant version of it (ENUM_FORMAT: CAPITAL_CASE)
     */
    public static String toEnumFormat(String name) {
        StringBuilder result = new StringBuilder(name.length() + 2);
        
        boolean capital = true;
        for (int i = 0, n = name.length(); i < n; i++) {
            char c = name.charAt(i);
            if (capital) {
                capital = Character.isUpperCase(c);
            } else if (Character.isUpperCase(c)) {
                capital = true;
                result.append('_');
            }
            result.append(capital ? c : Character.toUpperCase(c));
        }
        
        return result.toString();
    }
    
    /**
     * Replaces any occurrence of toReplace with another string.
     * Any colours that occured before the occurence of toReplace, are copied to the end of the replacement.
     *
     * @param target    The String to query
     * @param toReplace The sequence to replace
     * @param with      the replacing sequence
     * @return the result
     */
    public static String replaceKeepColours(String target, String toReplace, String with) {
        int index = -toReplace.length();
        while ((index = target.indexOf(toReplace, index + toReplace.length())) != -1) {
            String start = target.substring(0, index);
            Formatting coloursBefore = Formatting.getFormats(start);
            String after;
            try {
                after = target.substring(index + toReplace.length());
            } catch (IndexOutOfBoundsException e) {
                after = "";
            }
            target = start + with + coloursBefore + after;
        }
        return target;
    }
    
    public static String replParam(String target, String param, Object repl) {
        return replParam(target, param, repl, false);
    }
    
    public static String replParams(String target, String[] params, Object[] repls) {
        return replParams(target, params, repls, false, false);
    }
    
    public static boolean replParams(String[] target, String[] params, Object[] repls) {
        return replParams(target, 0, target.length, params, repls);
    }
    
    public static boolean replParams(String[] target, int from, int to, String[] params, Object[] repls) {
        return replParams(target, from, to, params, repls, false);
    }
    
    public static boolean replParams(List<String> target, String[] params, Object[] repls) {
        return replParams(target, 0, target.size(), params, repls);
    }
    
    public static boolean replParams(List<String> target, int from, int to, String[] params, Object[] repls) {
        return replParams(target, from, to, params, repls, false);
    }
    
    public static String replParamAndTranslate(String target, String param, Object repl) {
        return replParam(target, param, repl, true);
    }
    
    public static String replParamsAndTranslate(String target, String[] params, Object[] repls) {
        return replParams(target, params, repls, false, true);
    }
    
    public static boolean replParamsAndTranslate(String[] target, String[] params, Object[] repls) {
        return replParamsAndTranslate(target, 0, target.length, params, repls);
    }
    
    public static boolean replParamsAndTranslate(String[] target, int from, int to, String[] params, Object[] repls) {
        return replParams(target, from, to, params, repls, true);
    }
    
    public static boolean replParamsAndTranslate(List<String> target, String[] params, Object[] repls) {
        return replParamsAndTranslate(target, 0, target.size(), params, repls);
    }
    
    public static boolean replParamsAndTranslate(List<String> target, int from, int to, String[] params, Object[] repls) {
        return replParams(target, from, to, params, repls, true);
    }
    
    private static String replParam(String target, String param, Object replacementObj, boolean translate) {
        int idx = target.indexOf(param, 0);
        if (idx == -1) {
            return translate ? Formatting.translate(target) : target;
        }
        
        String rep = replacementObj.toString();
        StringBuilder builder = new StringBuilder(target);
        do {
            builder.replace(idx, idx + param.length(), rep);
            idx = builder.indexOf(param, idx + rep.length());
        } while (idx != -1);
        
        if (translate) {
            Formatting.translate(builder);
        }
        
        return builder.toString();
    }
    
    @SuppressWarnings("StringEquality")
    private static boolean replParams(String[] target, int from, int to, String[] params, Object[] repls, boolean translate) {
        if (from < 0 || to < from || to > target.length) {
            throw new IllegalArgumentException("Invalid from-to for array size " + target.length + ": " + from + "-" + to);
        }
        
        boolean change = false;
        for (int i = from; i < to; i++) {
            String val = target[i];
            if (val != (val = replParams(val, params, repls, true, translate))) {
                target[i] = val;
                change = true;
            }
        }
        return change;
    }
    
    @SuppressWarnings("StringEquality")
    private static boolean replParams(List<String> target, int from, int to, String[] params, Object[] repls, boolean translate) {
        if (from < 0 || to < from || to > target.size()) {
            throw new IllegalArgumentException("Invalid from-to for list size " + target.size() + ": " + from + "-" + to);
        }
    
        boolean change = false;
        if (target instanceof RandomAccess) {
            for (int i = from; i < to; i++) {
                String val = target.get(i);
                if (val != (val = replParams(val, params, repls, true, translate))) {
                    target.set(i, val);
                    change = true;
                }
            }
        } else {
            ListIterator<String> itr = target.listIterator(from);
            for (int n = to - from, i = 0; i < n && itr.hasNext(); i++) {
                String val = itr.next();
                if (val != (val = replParams(val, params, repls, true, translate))) {
                    itr.set(val);
                    change = true;
                }
            }
        }
        return change;
    }
    
    private static String replParams(String target, String[] params, Object[] repls, boolean updateRepls, boolean translate) {
        int n = params.length;
        if (n != repls.length) {
            throw new IllegalArgumentException();
        }
        
        String param = null;
        int idx = -1;
        int i;
        for (i = 0; i < n; i++) {
            param = params[i];
            if (param == null) {
                continue;
            }
            idx = target.indexOf(param, 0);
            if (idx != -1) {
                break;
            }
        }
        
        if (idx == -1) {
            return translate ? Formatting.translate(target) : target;
        }
        
        String repl = repls[i].toString();
        if (updateRepls) {
            repls[i] = repl;
        }
        
        StringBuilder builder = new StringBuilder(target);
        do {
            builder.replace(idx, idx + param.length(), repl);
            idx = builder.indexOf(param, idx + repl.length());
        } while (idx != -1);
        
        for (i++; i < n; i++) {
            param = params[i];
            if (param == null || (idx = builder.indexOf(param, 0)) == -1) {
                continue;
            }
            
            repl = repls[i].toString();
            if (updateRepls) {
                repls[i] = repl;
            }
            
            do {
                builder.replace(idx, idx + param.length(), repl);
                idx = builder.indexOf(param, idx + repl.length());
            } while (idx != -1);
        }
        
        if (translate) {
            Formatting.translate(builder);
        }
        
        return builder.toString();
    }
    
}
