package io.dico.dicore.command;

import io.dico.dicore.Formatting;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.ContextParser;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.ParameterList;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * The context of execution.
 * <p>
 * This class is responsible for the control flow of parameter parsing, as well as caching and providing the parsed parameter values.
 * It is also responsible for keeping track of the parameter to complete in the case of a tab completion.
 */
public class ExecutionContext {
    // Sender of the command
    private final CommandSender sender;
    // Address while parsing parameters with ContextParser
    private ICommandAddress address;
    // Command to execute
    private Command command;
    // if this flag is set, this execution is only for completion purposes.
    private boolean tabComplete;

    private final ArgumentBuffer buffer;
    // private ArgumentBuffer processedBuffer;

    // caches the buffer's cursor before parsing. This is needed to provide the original input of the player.
    private int cursorStart;

    // when the context starts parsing parameters, this flag is set, and any subsequent calls to #parseParameters() throw an IllegalStateException.
    //private boolean attemptedToParse;


    // The parsed parameter values, mapped by parameter name.
    // This also includes default values. All parameters from the parameter list are present if parsing was successful.
    private Map<String, Object> parameterValueMap = new HashMap<>();
    // this set contains the names of the parameters that were present in the command, and not given a default value.
    private Set<String> parsedParameters = new HashSet<>();


    // these fields store information required to provide completions.
    // the parameter to complete is the parameter that threw an exception when it was parsing.
    // the exception's message was discarded because it is a completion.
    private Parameter<?, ?> parameterToComplete;
    // this is the cursor that the ArgumentBuffer is reset to when suggested completions are requested.
    private int parameterToCompleteCursor = -1;

    // if this flag is set, any messages sent through the sendMessage methods are discarded.
    private boolean muted;

    public ExecutionContext(CommandSender sender, ArgumentBuffer buffer, boolean tabComplete) {
        this.sender = Objects.requireNonNull(sender);
        this.buffer = Objects.requireNonNull(buffer);
        this.muted = tabComplete;
        this.tabComplete = tabComplete;

        // If its tab completing, keep the empty element that might be at the end of the buffer
        // due to a space at the end of the command.
        // This allows the parser to correctly identify the parameter to be completed in this case.
        if (!tabComplete) {
            buffer.dropTrailingEmptyElements();
        }
    }

    /**
     * Construct an execution context that is ready to parse the parameter values.
     *
     * @param sender      the sender
     * @param address     the address
     * @param command     the command
     * @param buffer      the arguments
     * @param tabComplete true if this execution is a tab-completion
     */
    public ExecutionContext(CommandSender sender, ICommandAddress address, Command command, ArgumentBuffer buffer, boolean tabComplete) {
        this(sender, buffer, tabComplete);
        setAddress(address);
        setCommand(command);
    }

    /**
     * Sender of the command
     *
     * @return the sender of the command
     */
    public CommandSender getSender() {
        return sender;
    }

    /**
     * @return the buffer of arguments
     */
    public ArgumentBuffer getBuffer() {
        return buffer;
    }

    /**
     * Command's address
     *
     * @return the command's address
     */
    public ICommandAddress getAddress() {
        return address;
    }

    /**
     * Set the address
     *
     * @param address the new address
     */
    public void setAddress(ICommandAddress address) {
        this.address = address;
    }

    /**
     * The command
     *
     * @return the command
     */
    public Command getCommand() {
        return command;
    }

    /**
     * Set the command
     *
     * @param command the new command
     */
    public void setCommand(Command command) {
        this.command = command;
    }

    /**
     * @return true if this context is for a tab completion.
     */
    public boolean isTabComplete() {
        return tabComplete;
    }

    /**
     * @return true if this context is muted.
     */
    public boolean isMuted() {
        return muted;
    }

    /**
     * Parse parameters from the given parameter list,
     * adding their values to the cache of this context.
     *
     * @param parameterList the parameterList
     * @throws CommandException if the arguments are not valid
     */
    public void parse(ParameterList parameterList) throws CommandException {
        cursorStart = buffer.getCursor();

        ContextParser parser = new ContextParser(this, parameterList, parameterValueMap, parsedParameters);

        try {
            parser.parse();
        } finally {
            if (tabComplete) {
                parameterToComplete = parser.getCompletionTarget();
                parameterToCompleteCursor = parser.getCompletionCursor();
            }
        }

    }

    /**
     * The command's parameter definition.
     *
     * @return the parameter list
     */
    @Deprecated
    public ParameterList getParameterList() {
        return null;//command.getParameterList();
    }

    /**
     * Get the buffer as it was before preprocessing the arguments.
     *
     * @return the original buffer
     */
    @Deprecated
    public ArgumentBuffer getOriginalBuffer() {
        return buffer;
    }

    /**
     * The arguments
     *
     * @return the argument buffer
     */
    @Deprecated
    public ArgumentBuffer getProcessedBuffer() {
        return buffer;
    }

    /**
     * The cursor start, in other words, the buffer's cursor before parameters were parsed.
     *
     * @return the cursor start
     */
    public int getCursorStart() {
        return cursorStart;
    }

    /**
     * The original arguments.
     *
     * @return original arguments.
     */
    public String[] getOriginal() {
        return buffer.getArrayFromIndex(cursorStart);
    }

    /**
     * The path used to access this address.
     *
     * @return the path used to access this address.
     */
    public String[] getRoute() {
        return Arrays.copyOf(buffer.toArray(), address.getDepth());
    }

    public Formatting getFormat(EMessageType type) {
        return address.getChatHandler().getChatFormatForType(type);
    }

    /**
     * The full command as cached by the buffer. Might be incomplete depending on how it was dispatched.
     *
     * @return the full command
     */
    public String getRawInput() {
        return buffer.getRawInput();
    }

    /**
     * Get the value of the parameter with the given name
     *
     * @param name the parameter's name
     * @param <T> expected type
     * @return the parsed value or the default value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        if (!parameterValueMap.containsKey(name)) {
            throw new IllegalArgumentException();
        }

        try {
            return (T) parameterValueMap.get(name);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException("Invalid type parameter requested for parameter " + name, ex);
        }
    }

    /**
     * Get the value of the flag with the given name
     *
     * @param flag the flag's name, without preceding "-"
     * @param <T> expected type
     * @return the parsed value or the default value
     */
    public <T> T getFlag(String flag) {
        return get("-" + flag);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public <T> T get(int index) {
        return null;//get(getParameterList().getIndexedParameterName(index));
    }

    /**
     * Checks if the parameter by the name was provided in the command's arguments.
     *
     * @param name the parameter name
     * @return true if it was provided
     */
    public boolean isProvided(String name) {
        return parsedParameters.contains(name);
    }

    /**
     * Checks if the parameter by the index was provided in the command's arguments.
     *
     * @param index the parameter index
     * @return true if it was provided
     */
    @Deprecated
    public boolean isProvided(int index) {
        return false;//isProvided(getParameterList().getIndexedParameterName(index));
    }

    /**
     * The parameter to complete.
     * This parameter is requested suggestions
     *
     * @return the parameter to complete.
     */
    public Parameter<?, ?> getParameterToComplete() {
        return parameterToComplete;
    }

    /**
     * Get suggested completions.
     *
     * @param location The location as passed to {link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}, or null if requested in another way.
     * @return completions.
     */
    public List<String> getSuggestedCompletions(Location location) {
        if (parameterToComplete != null) {
            return parameterToComplete.complete(this, location, buffer.getUnaffectingCopy().setCursor(parameterToCompleteCursor));
        }

        List<String> result = new ArrayList<>();
        for (String name : parameterValueMap.keySet()) {
            if (name.startsWith("-") && !parsedParameters.contains(name)) {
                result.add(name);
            }
        }
        return result;
    }

    /*
    Chat handling
     */

    public void sendMessage(String message) {
        sendMessage(true, message);
    }

    public void sendMessage(EMessageType messageType, String message) {
        sendMessage(messageType, true, message);
    }

    public void sendMessage(boolean translateColours, String message) {
        sendMessage(EMessageType.NEUTRAL, translateColours, message);
    }

    public void sendMessage(EMessageType messageType, boolean translateColours, String message) {
        if (!muted) {
            if (translateColours) {
                message = Formatting.translateChars('&', message);
            }
            address.getChatHandler().sendMessage(this, messageType, message);
        }
    }

    public void sendMessage(String messageFormat, Object... args) {
        sendMessage(true, messageFormat, args);
    }

    public void sendMessage(EMessageType messageType, String messageFormat, Object... args) {
        sendMessage(messageType, true, messageFormat, args);
    }

    public void sendMessage(boolean translateColours, String messageFormat, Object... args) {
        sendMessage(EMessageType.NEUTRAL, translateColours, messageFormat, args);
    }

    public void sendMessage(EMessageType messageType, boolean translateColours, String messageFormat, Object... args) {
        sendMessage(messageType, translateColours, String.format(messageFormat, args));
    }

    public void sendHelpMessage(int page) {
        if (!muted) {
            address.getChatHandler().sendHelpMessage(sender, this, address, page);
        }
    }

    public void sendSyntaxMessage() {
        if (!muted) {
            address.getChatHandler().sendSyntaxMessage(sender, this, address);
        }
    }

}
