package io.dico.dicore.command.example;

import io.dico.dicore.command.CommandBuilder;
import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.Validate;
import io.dico.dicore.command.annotation.Cmd;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.type.ParameterConfig;
import io.dico.dicore.command.parameter.type.ParameterType;
import org.bukkit.command.CommandSender;

public class ParameterInfoObjectExample {

    private @interface ParentPermission {
        String value();
    }

    static class MyInfoObject {
        public static final ParameterConfig<ParentPermission, MyInfoObject> config = new ParameterConfig<ParentPermission, MyInfoObject>() {
            @Override
            protected MyInfoObject toParameterInfo(ParentPermission annotation) {
                return new MyInfoObject(annotation.value());
            }
        };

        private String permissionParent;

        MyInfoObject(String permissionParent) {
            this.permissionParent = permissionParent;
        }

        public String getPermissionParent() {
            return permissionParent;
        }
    }

    static class MyParameterType extends ParameterType<String, MyInfoObject> {

        public MyParameterType() {
            super(String.class, MyInfoObject.config);
        }

        @Override
        public String parse(Parameter<String, MyInfoObject> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
            String value = buffer.next();

            MyInfoObject mio = parameter.getParamInfo();
            if (mio != null) {
                String permission = mio.permissionParent + "." + value;
                Validate.isAuthorized(sender, permission);
            }

            return value;
        }
    }

    static class MyCommands {

        @Cmd("test")
        Object cmdTest(@ParentPermission("test.permission") String value) {
            return "You have permission to use the argument '" + value + "'!";
        }

    }

    static void main(String[] args) {
        new CommandBuilder()
            .addParameterType(false, new MyParameterType())
            .registerCommands(new MyCommands());
    }

}
