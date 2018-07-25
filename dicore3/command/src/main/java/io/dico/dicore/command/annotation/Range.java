package io.dico.dicore.command.annotation;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.Validate;
import io.dico.dicore.command.parameter.type.ParameterConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Range {
    Class<?> MEMORY_CLASS = Memory.class;
    ParameterConfig<Range, Memory> CONFIG = ParameterConfig.getMemoryClassFromField(Range.class);
    Memory DEFAULT = new Memory(-Double.MAX_VALUE, Double.MAX_VALUE, 0);

    double min() default -Double.MAX_VALUE;

    double max() default Double.MAX_VALUE;

    double defaultValue() default 0;

    class Memory {
        private final double min;
        private final double max;
        private final double defaultValue;

        public Memory(Range range) {
            this(range.min(), range.max(), range.defaultValue());
        }

        public Memory(double min, double max, double defaultValue) {
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        public double defaultValue() {
            return defaultValue;
        }

        public void validate(Number x, String failMessage) throws CommandException {
            Validate.isTrue(valid(x), failMessage);
        }

        public boolean valid(Number x) {
            double d = x.doubleValue();
            return min <= d && d <= max;
        }

        public boolean isDefault() {
            return this == DEFAULT || (min == DEFAULT.min && max == DEFAULT.max && defaultValue == DEFAULT.defaultValue);
        }

    }

}
