package io.dico.dicore.command;

import io.dico.dicore.command.chat.Formatting;
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
    private CommandSender sender;
    private ICommandAddress address;
    private Command command;
    private ArgumentBuffer originalBuffer;
    private ArgumentBuffer processedBuffer;

    // caches the buffer's cursor before parsing. This is needed to provide the original input of the player.
    private int cursorStart;

    // when the context starts parsing parameters, this flag is set, and any subsequent calls to #parseParameters() throw an IllegalStateException.
    private boolean attemptedToParse;


    // The parsed parameter values, mapped by parameter name.
    // This also includes default values. All parameters from the parameter list are present if parsing was successful.
    private Map<String, Object> parameterValueMap;
    // this set contains the names of the parameters that were present in the command, and not given a default value.
    private Set<String> parsedParameters;

    // if this flag is set, this execution is only for completion purposes.
    private boolean tabComplete;
    // these fields store information required to provide completions.
    // the parameter to complete is the parameter that threw an exception when it was parsing.
    // the exception's message was discarded because it is a completion.
    private Parameter<?, ?> parameterToComplete;
    // this is the cursor that the ArgumentBuffer is reset to when suggested completions are requested.
    private int parameterToCompleteCursor = -1;

    // if this flag is set, any messages sent through the sendMessage methods are discarded.
    private boolean muted;

    public ExecutionContext(CommandSender sender, boolean tabComplete) {
        this.sender = Objects.requireNonNull(sender);
        this.muted = tabComplete;
        this.tabComplete = tabComplete;
    }

    /**
     * Construct an execution context that is ready to parse the parameter values.
     *
     * @param sender      the sender
     * @param address     the address
     * @param buffer      the arguments
     * @param tabComplete true if this execution is a tab-completion
     */
    public ExecutionContext(CommandSender sender, ICommandAddress address, Command command, ArgumentBuffer buffer, boolean tabComplete) {
        this(sender, tabComplete);
        targetAcquired(address, command, buffer);
    }

    void requireAddressPresent(boolean present) {
        //noinspection DoubleNegation
        if ((address != null) != present) {
            throw new IllegalStateException();
        }
    }

    void targetAcquired(ICommandAddress address, Command command, ArgumentBuffer buffer) {
        requireAddressPresent(false);

        this.address = Objects.requireNonNull(address);
        this.command = Objects.requireNonNull(command);

        // If its tab completing, keep the empty element that might be at the end of the buffer
        // due to a space at the end of the command.
        // This allows the parser to correctly identify the parameter to be completed in this case.
        if (!tabComplete) {
            buffer.dropTrailingEmptyElements();
        }

        this.originalBuffer = buffer;
        this.processedBuffer = buffer.preprocessArguments(getParameterList().getArgumentPreProcessor());
        this.cursorStart = buffer.getCursor();
    }

    /**
     * Parse the parameters. If no exception is thrown, they were parsed successfully, and the command may continue post-parameter execution.
     *
     * @throws CommandException if an error occurs while parsing the parameters.
     */
    synchronized void parseParameters() throws CommandException {
        requireAddressPresent(true);
        if (attemptedToParse) {
            throw new IllegalStateException();
        }

        attemptedToParse = true;

        ContextParser parser = new ContextParser(this);

        parameterValueMap = parser.getValueMap();
        parsedParameters = parser.getParsedKeys();

        parser.parse();
    }


    /**
     * Attempts to parse parameters, without throwing an exception or sending any message.
     * This method is typically used by tab completions.
     * After calling this method, the context is ready to provide completions.
     */
    synchronized void parseParametersQuietly() {
        requireAddressPresent(true);
        if (attemptedToParse) {
            throw new IllegalStateException();
        }

        attemptedToParse = true;

        boolean before = muted;
        muted = true;
        try {
            ContextParser parser = new ContextParser(this);

            parameterValueMap = parser.getValueMap();
            parsedParameters = parser.getParsedKeys();

            parser.parse();

            parameterToComplete = parser.getCompletionTarget();
            parameterToCompleteCursor = parser.getCompletionCursor();

        } catch (CommandException ignored) {

        } finally {
            muted = before;
        }
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
     * Command's address
     *
     * @return the command's address
     */
    public ICommandAddress getAddress() {
        return address;
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
     * The command's parameter definition.
     *
     * @return the parameter list
     */
    public ParameterList getParameterList() {
        return command.getParameterList();
    }

    /**
     * Get the buffer as it was before preprocessing the arguments.
     *
     * @return the original buffer
     */
    public ArgumentBuffer getOriginalBuffer() {
        return originalBuffer;
    }

    /**
     * The arguments
     *
     * @return the argument buffer
     */
    public ArgumentBuffer getProcessedBuffer() {
        return processedBuffer;
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
        return originalBuffer.getArrayFromIndex(cursorStart);
    }

    /**
     * The path used to access this address.
     *
     * @return the path used to access this address.
     */
    public String[] getRoute() {
        return Arrays.copyOf(originalBuffer.toArray(), address.getDepth());
    }

    public Formatting getFormat(EMessageType type) {
        return address.getChatController().getChatFormatForType(type);
    }

    /**
     * The full command as cached by the buffer. Might be incomplete depending on how it was dispatched.
     *
     * @return the full command
     */
    public String getRawInput() {
        return originalBuffer.getRawInput();
    }

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

    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        return get(getParameterList().getIndexedParameterName(index));
    }

    public <T> T getFlag(String flag) {
        return get("-" + flag);
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
    public boolean isProvided(int index) {
        return isProvided(getParameterList().getIndexedParameterName(index));
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
     * @return true if this context is muted.
     */
    public boolean isMuted() {
        return muted;
    }

    /**
     * @return true if this context is for a tab completion.
     */
    public boolean isTabComplete() {
        return tabComplete;
    }

    /**
     * Get suggested completions.
     *
     * @param location The location as passed to {link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}, or null if requested in another way.
     * @return completions.
     */
    public List<String> getSuggestedCompletions(Location location) {
        if (parameterToComplete != null) {
            return parameterToComplete.complete(this, location, processedBuffer.getUnaffectingCopy().setCursor(parameterToCompleteCursor));
        }

        ParameterList parameterList = getParameterList();
        List<String> result = new ArrayList<>();
        for (String name : parameterValueMap.keySet()) {
            if (parameterList.getParameterByName(name).isFlag() && !parsedParameters.contains(name)) {
                result.add(name);
            }
        }
        return result;
    }

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
            address.getChatController().sendMessage(this, messageType, message);
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
            address.getChatController().sendHelpMessage(sender, this, address, page);
        }
    }

    public void sendSyntaxMessage() {
        if (!muted) {
            address.getChatController().sendSyntaxMessage(sender, this, address);
        }
    }

}
