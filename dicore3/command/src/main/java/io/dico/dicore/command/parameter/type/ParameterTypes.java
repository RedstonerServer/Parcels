package io.dico.dicore.command.parameter.type;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.Validate;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.Parameter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Class providing default parameter types
 */
public class ParameterTypes {

    public static IParameterTypeSelector getSelector() {
        return MapBasedParameterTypeSelector.defaultSelector;
    }

    private static <T> T registerType(boolean infolessAlias, T obj) {
        getSelector().addType(infolessAlias, (ParameterType<?, ?>) obj);
        return obj;
    }

    static void clinit() {
        // initializes class
    }

    public static final ParameterType<String, Void> STRING;
    public static final ParameterType<Boolean, Void> BOOLEAN;
    public static final NumberParameterType<Double> DOUBLE;
    public static final NumberParameterType<Integer> INTEGER;
    public static final NumberParameterType<Long> LONG;
    public static final NumberParameterType<Short> SHORT;
    public static final NumberParameterType<Float> FLOAT;
    public static final ParameterType<Player, Void> PLAYER;
    public static final ParameterType<OfflinePlayer, Void> OFFLINE_PLAYER;

    private ParameterTypes() {

    }

    static {
        STRING = registerType(false, new SimpleParameterType<String, Void>(String.class) {
            @Override
            protected String parse(Parameter<String, Void> parameter, CommandSender sender, String input) throws CommandException {
                return input;
            }

            @Override
            public String getDefaultValue(Parameter<String, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                return null;
            }
        });

        BOOLEAN = registerType(true, new ParameterType<Boolean, Void>(Boolean.TYPE) {
            @Override
            public Boolean parse(Parameter<Boolean, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                String input = buffer.requireNext(parameter.getName());
                switch (input.toLowerCase()) {
                    case "true":
                    case "yes":
                        return true;
                    case "false":
                    case "no":
                        return false;
                    default:
                        throw CommandException.invalidArgument(parameter.getName(), "true, false, yes or no");
                }
            }

            @Override
            public Boolean getDefaultValue(Parameter<Boolean, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                return !parameter.isPrimitive() ? null : false;
            }

            @Override
            public List<String> complete(Parameter<Boolean, Void> parameter, CommandSender sender, Location location, ArgumentBuffer buffer) {
                String input = buffer.next();
                if (input != null) {
                    List<String> result = new ArrayList<>(1);
                    input = input.toLowerCase();
                    for (String value : new String[]{"true", "yes", "false", "no"}) {
                        if (value.startsWith(input)) {
                            result.add(value);
                        }
                    }
                    return result;
                }
                return Arrays.asList("true", "yes", "false", "no");
            }

            @Override
            protected FlagParameterType<Boolean, Void> flagTypeParameter() {
                return new FlagParameterType<Boolean, Void>(this) {
                    @Override
                    public Boolean parse(Parameter<Boolean, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                        return true;
                    }

                    @Override
                    public Boolean getDefaultValue(Parameter<Boolean, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                        return false;
                    }

                    @Override
                    public int getExpectedAmountOfConsumedArguments() {
                        return 0;
                    }
                };
            }
        });

        INTEGER = registerType(true, new NumberParameterType<Integer>(Integer.TYPE) {
            @Override
            protected Integer parse(String input) throws NumberFormatException {
                return Integer.parseInt(input);
            }

            @Override
            protected Integer select(Number number) {
                return number.intValue();
            }
        });

        DOUBLE = registerType(true, new NumberParameterType<Double>(Double.TYPE) {
            @Override
            protected Double parse(String input) throws NumberFormatException {
                return Double.parseDouble(input);
            }

            @Override
            protected Double select(Number number) {
                return number.doubleValue();
            }
        });

        LONG = registerType(true, new NumberParameterType<Long>(Long.TYPE) {
            @Override
            protected Long parse(String input) throws NumberFormatException {
                return Long.parseLong(input);
            }

            @Override
            protected Long select(Number number) {
                return number.longValue();
            }
        });

        SHORT = registerType(true, new NumberParameterType<Short>(Short.TYPE) {
            @Override
            protected Short parse(String input) throws NumberFormatException {
                return Short.parseShort(input);
            }

            @Override
            protected Short select(Number number) {
                return number.shortValue();
            }
        });

        FLOAT = registerType(true, new NumberParameterType<Float>(Float.TYPE) {
            @Override
            protected Float parse(String input) throws NumberFormatException {
                return Float.parseFloat(input);
            }

            @Override
            protected Float select(Number number) {
                return number.floatValue();
            }
        });

        PLAYER = registerType(true, new SimpleParameterType<Player, Void>(Player.class) {
            @Override
            protected Player parse(Parameter<Player, Void> parameter, CommandSender sender, String input) throws CommandException {
                //System.out.println("In ParameterTypes#PLAYER.parse()");
                Player player = Bukkit.getPlayer(input);
                Validate.notNull(player, "A player by the name '" + input + "' could not be found");
                return player;
            }

            @Override
            public List<String> complete(Parameter<Player, Void> parameter, CommandSender sender, Location location, ArgumentBuffer buffer) {
                String input = buffer.nextOrEmpty().toLowerCase();
                List<String> result = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        result.add(player.getName());
                    }
                }
                return result;
            }
        });

        OFFLINE_PLAYER = registerType(true, new SimpleParameterType<OfflinePlayer, Void>(OfflinePlayer.class) {
            @Override
            protected OfflinePlayer parse(Parameter<OfflinePlayer, Void> parameter, CommandSender sender, String input) throws CommandException {
                OfflinePlayer result = Bukkit.getPlayer(input);
                if (result != null) {
                    return result;
                }

                input = input.toLowerCase(Locale.ROOT);
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (offlinePlayer.getName().toLowerCase(Locale.ROOT).startsWith(input)) {
                        return offlinePlayer;
                    }
                }

                throw new CommandException("An offline player by the name '" + input + "' could not be found");
            }

            @Override
            public List<String> complete(Parameter<OfflinePlayer, Void> parameter, CommandSender sender, Location location, ArgumentBuffer buffer) {
                String input = buffer.nextOrEmpty().toLowerCase();
                ArrayList<String> result = new ArrayList<>();
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        if (player.isOnline()) {
                            result.add(0, player.getName());
                        } else {
                            result.add(player.getName());
                        }
                    }
                }
                return result;
            }
        });
        
        /*
        PRESENCE = registerType(false, new ParameterType<Boolean, Void>(Boolean.class) {
            @Override
            public Boolean parse(IParameter<Boolean, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                return null;
            }
    
            @Override
            protected FlagParameterType<Boolean, Void> flagTypeParameter() {
                return new FlagParameterType<Boolean, Void>(this) {
                    @Override
                    public Boolean parse(IParameter<Boolean, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                        return true;
                    }
    
                    @Override
                    public Boolean getDefaultValue(IParameter<Boolean, Void> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
                        return false;
                    }
    
                    @Override
                    public int getExpectedAmountOfConsumedArguments() {
                        return 0;
                    }
                };
            }
        });
        */

    }

}
