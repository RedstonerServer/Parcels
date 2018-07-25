package io.dico.dicore.command.annotation;

import io.dico.dicore.command.parameter.type.ParameterConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface BigRange {
    Class<?> MEMORY_CLASS = Memory.class;
    ParameterConfig<BigRange, Memory> CONFIG = ParameterConfig.getMemoryClassFromField(BigRange.class);
    Memory DEFAULT = new Memory("MIN", "MAX", "0");

    String min() default "MIN";

    String max() default "MAX";

    String defaultValue() default "0";

    class Memory {
        private final String min;
        private final String max;
        private final String defaultValue;

        public Memory(BigRange range) {
            this(range.min(), range.max(), range.defaultValue());
        }

        public Memory(String min, String max, String defaultValue) {
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }

        public String min() {
            return min;
        }

        public String max() {
            return max;
        }

        public String defaultValue() {
            return defaultValue;
        }

    }

}
