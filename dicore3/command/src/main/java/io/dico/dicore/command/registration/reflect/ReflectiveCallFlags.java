package io.dico.dicore.command.registration.reflect;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.exceptions.checkedfunctions.CheckedSupplier;

/**
 * Call flags store which extra parameters the target function expects on top of command parameters.
 * All 4 possible extra parameters are listed below.
 * <p>
 * Extra parameters are ordered by the bit that represents them in the call flags.
 * They can either be leading or trailing the command's parameters.
 */
public class ReflectiveCallFlags {

    /**
     * Receiver ({@code this} in some kotlin functions - always first parameter)
     *
     * @see ICommandInterceptor#getReceiver(io.dico.dicore.command.ExecutionContext, java.lang.reflect.Method, String)
     */
    public static final int RECEIVER_BIT = 1 << 0;

    /**
     * CommandSender
     *
     * @see org.bukkit.command.CommandSender
     */
    public static final int SENDER_BIT = 1 << 1;

    /**
     * ExecutionContext
     *
     * @see io.dico.dicore.command.ExecutionContext
     */
    public static final int CONTEXT_BIT = 1 << 2;

    /**
     * Continuation (trailing parameters of kotlin suspended functions)
     *
     * @see kotlin.coroutines.Continuation
     */
    public static final int CONTINUATION_BIT = 1 << 3;

    /**
     * Mask of extra parameters that trail the command's parameters, instead of leading.
     */
    public static final int TRAILING_MASK = CONTINUATION_BIT;

    /**
     * Check if the call arg is trailing the command's parameters.
     *
     * @param bit the bit used for the call flag
     * @return true if the call arg is trailing the command's parameters
     */
    public static boolean isTrailingCallArg(int bit) {
        return (bit & TRAILING_MASK) != 0;
    }

    /**
     * Number of call arguments leading the command parameters.
     *
     * @param flags the call flags
     * @return the number of call arguments leading the command parameters
     */
    public static int getLeadingCallArgNum(int flags) {
        return Integer.bitCount(flags & ~TRAILING_MASK);
    }

    /**
     * Number of call arguments trailing the command parameters.
     *
     * @param flags the call flags
     * @return the number of call arguments trailing the command parameters
     */
    public static int getTrailingCallArgNum(int flags) {
        return Integer.bitCount(flags & TRAILING_MASK);
    }

    /**
     * Check if the flags contain the call arg.
     *
     * @param flags the call flags
     * @param bit   the bit used for the call flag
     * @return true if the flags contain the call arg
     */
    public static boolean hasCallArg(int flags, int bit) {
        return (flags & bit) != 0;
    }

    /**
     * Get the index used for the call arg when calling the reflective function
     *
     * @param flags           the call flags
     * @param bit             the bit used for the call flag
     * @param cmdParameterNum the number of parameters of the command
     * @return the index used for the call arg
     */
    public static int getCallArgIndex(int flags, int bit, int cmdParameterNum) {
        if ((bit & TRAILING_MASK) == 0) {
            // Leading.

            int preceding = precedingMaskFrom(bit);
            int mask = flags & precedingMaskFrom(bit) & ~TRAILING_MASK;

            // Count the number of present call args that are leading and precede the given bit
            return Integer.bitCount(flags & precedingMaskFrom(bit) & ~TRAILING_MASK);
        } else {
            // Trailing.

            // Count the number of present call args that are leading
            // plus the number of present call args that are trailing and precede the given bit
            // plus the command's parameters

            return Integer.bitCount(flags & ~TRAILING_MASK)
                + Integer.bitCount(flags & precedingMaskFrom(bit) & TRAILING_MASK)
                + cmdParameterNum;
        }
    }

    /**
     * Get the mask for all bits trailing the given fromBit
     *
     * <p>
     * For example, if the bit is 00010000
     * This function returns 00001111
     * <p>
     *
     * @param fromBit number with the bit set there the ones should stop.
     * @return the mask for all bits trailing the given fromBit
     */
    private static int precedingMaskFrom(int fromBit) {
        int trailingZeros = Integer.numberOfTrailingZeros(fromBit);
        if (trailingZeros == 0) return 0;
        return -1 >>> -trailingZeros;
    }

    /**
     * Get the object array used to call the function.
     *
     * @param callFlags        the call flags
     * @param context          the context
     * @param parameterOrder   the order of parameters in the function
     * @param receiverFunction the function that will create the receiver for this call, if applicable
     * @return the call args
     */
    public static Object[] getCallArgs(
        int callFlags,
        ExecutionContext context,
        String[] parameterOrder,
        CheckedSupplier<Object, CommandException> receiverFunction
    ) throws CommandException {
        int leadingParameterNum = getLeadingCallArgNum(callFlags);
        int cmdParameterNum = parameterOrder.length;
        int trailingParameterNum = getTrailingCallArgNum(callFlags);

        Object[] result = new Object[leadingParameterNum + cmdParameterNum + trailingParameterNum];

        if (hasCallArg(callFlags, RECEIVER_BIT)) {
            int index = getCallArgIndex(callFlags, RECEIVER_BIT, cmdParameterNum);
            result[index] = receiverFunction.get();
        }

        if (hasCallArg(callFlags, SENDER_BIT)) {
            int index = getCallArgIndex(callFlags, SENDER_BIT, cmdParameterNum);
            result[index] = context.getSender();
        }

        if (hasCallArg(callFlags, CONTEXT_BIT)) {
            int index = getCallArgIndex(callFlags, CONTEXT_BIT, cmdParameterNum);
            result[index] = context;
        }

        if (hasCallArg(callFlags, CONTINUATION_BIT)) {
            int index = getCallArgIndex(callFlags, CONTINUATION_BIT, cmdParameterNum);
            result[index] = null; // filled in later.
        }

        for (int i = 0; i < parameterOrder.length; i++) {
            String parameterName = parameterOrder[i];
            result[leadingParameterNum + i] = context.get(parameterName);
        }

        return result;
    }

}
