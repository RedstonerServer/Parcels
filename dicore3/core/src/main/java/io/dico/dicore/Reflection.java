package io.dico.dicore;

import io.dico.dicore.exceptions.ExceptionHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reflective utilities
 */
@SuppressWarnings("unchecked")
public class Reflection {
    private static final ExceptionHandler exceptionHandler;
    private static final Field fieldModifiersField = restrictedSearchField(Field.class, "modifiers");
    private static Consumer<String> errorTarget;
    
    private Reflection() {
        
    }
    
    static {
        exceptionHandler = new ExceptionHandler() {
            @Override
            public void handle(Throwable ex) {
                handleGenericException(ex);
            }
            
            @Override
            public Object handleGenericException(Throwable ex, Object... args) {
                String action = args.length == 0 || !(args[0] instanceof String) ? "executing a reflective operation" : (String) args[0];
                ExceptionHandler.log(errorTarget, action, ex);
                return null;
            }
        };
        
        // don't use method reference here: the current reference in System.out would be cached.
        setErrorTarget(msg -> System.out.println(msg));
    }
    
    /**
     * Sets the output where ReflectiveOperationException's and similar are sent.
     * This defaults to {@link System#out}.
     *
     * @param target The new output
     * @throws NullPointerException if target is null
     */
    public static void setErrorTarget(Consumer<String> target) {
        errorTarget = Objects.requireNonNull(target);
    }
    
    /**
     * This search modifier tells the implementation that it should subsequently search superclasses for the field/method.
     * Using this modifier means a call to {@link #deepSearchField(Class, String)} will be used instead of {@link #restrictedSearchField(Class, String)}
     * and a call to {@link #deepSearchMethod(Class, String, Class[])} will be used instead of {@link #restrictedSearchMethod(Class, String, Class[])}
     */
    public static final int DEEP_SEARCH = 0x1;
    
    /**
     * This search modifier applies only to fields, and tells the implementation that a final modifier might be present on a found field, and that it should be removed.
     */
    public static final int REMOVE_FINAL = 0x2;
    
    /**
     * This search modifier applies only to methods, and tells the implementation that it should completely ignore parameter types and return the first method with a matching name
     * The implementation uses {@link Class#getDeclaredMethods()} instead of {@link Class#getDeclaredMethod(String, Class[])} if this modifier is set.
     */
    public static final int IGNORE_PARAMS = 0x2;

    /*
    ### FIELD METHODS ###
     */
    
    /**
     * Search a field of any accessibility within the class or any of its superclasses.
     * The first field with the given name that is found will be returned.
     * <p>
     * If a field is found and it is not accessible, this method attempts to make it accessible.
     * If a {@link SecurityException} is thrown in the process, that is ignored and the field will be returned nonetheless.
     * <p>
     * This method throws IllegalArgumentException if the Field is not found, because, in most cases, that should never happen,
     * and it should simplify debugging. In some cases, if you want to know if the field exists, you'll have to use try/catch for that.
     *
     * @param clazz     The lowest class in the ladder to start searching from
     * @param fieldName The name of the field
     *                  //@param fieldType the type of the field, or null if it can be any.
     * @return The field
     * @throws NullPointerException     if clazz is null or fieldName is null
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #restrictedSearchField(Class, String)
     */
    public static Field deepSearchField(Class<?> clazz, String fieldName/*, Class<?> fieldType*/) {
        Class<?> currentClass = clazz;
        Field result;
        do {
            // throws NPE if class or fieldName is null
            result = internalSearchField(clazz, fieldName);
            if (result != null) {
                return result;
            }
            currentClass = currentClass.getSuperclass();
        } while (currentClass != null);
        
        throw new IllegalArgumentException("field not found in " + clazz.getCanonicalName() + " and superclasses: " + fieldName);
    }
    
    /**
     * Search a field of any accessibility within the class, but not its superclasses.
     * <p>
     * If a field is found and it is not accessible, this method attempts to make it accessible.
     * If a {@link SecurityException} is thrown in the process, that is ignored and the field will be returned nonetheless.
     * <p>
     * This method throws IllegalArgumentException if the Field is not found, because, in most cases, that should never happen,
     * and it should simplify debugging. In some cases, if you want to know if the field exists, you'll have to use try/catch for that.
     *
     * @param clazz     The only class to search for the field
     * @param fieldName The name of the field
     * @return The field
     * @throws NullPointerException     if clazz or fieldName is null
     * @throws IllegalArgumentException if the field does not exist
     */
    public static Field restrictedSearchField(Class<?> clazz, String fieldName) {
        Field result = internalSearchField(clazz, fieldName);
        if (result == null) {
            throw new IllegalArgumentException("field not found in " + clazz.getCanonicalName() + ": " + fieldName);
        }
        return result;
    }
    
    /**
     * Searches for a field using the given search method.
     *
     * @param modifiers The modifiers for field search. Can have {@link #DEEP_SEARCH} and {@link #REMOVE_FINAL}
     * @param clazz     The class to search in/from
     * @param fieldName Name of the field
     * @return The field
     * @throws NullPointerException     if clazz or fieldName is null
     * @throws IllegalArgumentException if the field is not found
     */
    public static Field searchField(int modifiers, Class<?> clazz, String fieldName) {
        Field result;
        if ((modifiers & DEEP_SEARCH) != 0) {
            result = deepSearchField(clazz, fieldName);
        } else {
            result = restrictedSearchField(clazz, fieldName);
        }
        if ((modifiers & REMOVE_FINAL) != 0) {
            removeFinalModifier(result);
        }
        return result;
    }
    
    /**
     * @return The same as {@link #restrictedSearchField(Class, String)}, but returns null instead of throwing IllegalArgumentException
     * @see #restrictedSearchField(Class, String)
     */
    private static Field internalSearchField(Class<?> clazz, String fieldName) {
        Field result;
        try {
            // throws NullPointerException if either clazz or fieldName are null.
            result = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException ex) {
            return null;
        }
        
        if (!result.isAccessible()) try {
            result.setAccessible(true);
        } catch (SecurityException ignored) {
            
        }
        
        return result;
    }
    
    /**
     * Attempts to remove existing final modifier of the given field
     * This method should always return true.
     *
     * @param field The field whose final modifier to remove
     * @return true if the field most definitely has no final modifier after this call
     * @throws NullPointerException if field is null
     */
    public static boolean removeFinalModifier(Field field) {
        Objects.requireNonNull(field);
        try {
            int modifiers = (int) fieldModifiersField.get(field);
            if (modifiers != (modifiers &= ~Modifier.FINAL)) {
                fieldModifiersField.set(field, modifiers);
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Gets field value of the field named fieldName and the given instance
     * To find the field, {@link #deepSearchField(Class, String)} is used (DEEP search method).
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param instance  The instance whose field value to get
     * @param fieldName the name of the field
     * @param <T>       The expected/known field type
     * @return The field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #deepSearchField(Class, String)
     * @see #getFieldValue(Class, String, Object)
     */
    public static <T> T getFieldValue(Object instance, String fieldName) {
        return getFieldValue(deepSearchField(instance.getClass(), fieldName), instance);
    }
    
    /**
     * Gets field value of the field named fieldName and the given instance
     * To find the field, {@link #restrictedSearchField(Class, String)} is used (RESTRICTED search method).
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param clazz     The class to search for the field
     * @param instance  The instance whose field value to get
     * @param fieldName the name of the field
     * @param <T>       The expected/known field type
     * @return The field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #restrictedSearchField(Class, String)
     * @see #getFieldValue(Field, Object)
     */
    public static <T> T getFieldValue(Class<?> clazz, String fieldName, Object instance) {
        return getFieldValue(restrictedSearchField(clazz, fieldName), instance);
    }
    
    /**
     * Gets field value of the field named fieldName and the given instance
     * To find the field, {@link #searchField(int, Class, String)} is used.
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param modifiers The modifiers for field search. Can have {@link #DEEP_SEARCH} and {@link #REMOVE_FINAL}
     * @param clazz     The class to search for the field
     * @param instance  The instance whose field value to get
     * @param fieldName the name of the field
     * @param <T>       The expected/known field type
     * @return The field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #searchField(int, Class, String)
     * @see #getFieldValue(Field, Object)
     */
    public static <T> T getFieldValue(int modifiers, Class<?> clazz, String fieldName, Object instance) {
        return getFieldValue(searchField(modifiers, clazz, fieldName), instance);
    }
    
    /**
     * Gets field value of the given field and the given instance
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param field    the field
     * @param instance The instance whose field value to get
     * @param <T>      The expected/known field type
     * @return The field value
     */
    public static <T> T getFieldValue(Field field, Object instance) {
        return exceptionHandler.supplySafe(() -> (T) field.get(instance));
    }
    
    /**
     * Gets static field value of the field named fieldName
     * To find the field, {@link #restrictedSearchField(Class, String)} is used (RESTRICTED search method).
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param clazz     The class to search for the field
     * @param fieldName the name of the field
     * @param <T>       The expected/known field type
     * @return The field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #restrictedSearchField(Class, String)
     * @see #getStaticFieldValue(Field)
     */
    public static <T> T getStaticFieldValue(Class<?> clazz, String fieldName) {
        return getStaticFieldValue(restrictedSearchField(clazz, fieldName));
    }
    
    /**
     * Gets static field value of the field named fieldName
     * To find the field, {@link #searchField(int, Class, String)} is used.
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param modifiers The modifiers for field search. Can have {@link #DEEP_SEARCH} and {@link #REMOVE_FINAL}
     * @param clazz     The class to search for the field
     * @param fieldName the name of the field
     * @param <T>       The expected/known field type
     * @return The field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #deepSearchField(Class, String)
     * @see #getStaticFieldValue(Field)
     */
    public static <T> T getStaticFieldValue(int modifiers, Class<?> clazz, String fieldName) {
        return getStaticFieldValue(searchField(modifiers, clazz, fieldName));
    }
    
    /**
     * Gets static field value
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     * <p>
     * Equivalent to the call {@code getFieldValue(field, (Object) null)}
     *
     * @param field the field
     * @param <T>   The expected/known field type
     * @return The field value
     * @see #getFieldValue(Field, Object)
     */
    public static <T> T getStaticFieldValue(Field field) {
        return getFieldValue(field, (Object) null);
    }
    
    /**
     * Sets field value of the field named fieldName and the given instance
     * To find the field, {@link #deepSearchField(Class, String)} is used (DEEP search method).
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param instance  The instance whose field value to set
     * @param fieldName the name of the field
     * @param newValue  the new field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #deepSearchField(Class, String)
     * @see #setFieldValue(Class, String, Object, Object)
     */
    public static void setFieldValue(Object instance, String fieldName, Object newValue) {
        setFieldValue(deepSearchField(instance.getClass(), fieldName), instance, newValue);
    }
    
    /**
     * Sets field value of the field named fieldName and the given instance
     * To find the field, {@link #restrictedSearchField(Class, String)} is used (RESTRICTED search method).
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param clazz     The class to search for the field
     * @param fieldName the name of the field
     * @param instance  The field owner
     * @param newValue  The new field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #restrictedSearchField(Class, String)
     * @see #setFieldValue(Field, Object, Object)
     */
    public static void setFieldValue(Class<?> clazz, String fieldName, Object instance, Object newValue) {
        setFieldValue(restrictedSearchField(clazz, fieldName), instance, newValue);
    }
    
    /**
     * Sets field value of the field named fieldName and the given instance
     * To find the field, {@link #searchField(int, Class, String)} is used.
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param modifiers The modifiers for field search. Can have {@link #DEEP_SEARCH} and {@link #REMOVE_FINAL}
     * @param clazz     The class to search for the field
     * @param instance  The instance whose field value to set
     * @param fieldName the name of the field
     * @param newValue  The new field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #searchField(int, Class, String)
     * @see #setFieldValue(Field, Object, Object)
     */
    public static void setFieldValue(int modifiers, Class<?> clazz, String fieldName, Object instance, Object newValue) {
        setFieldValue(searchField(modifiers, clazz, fieldName), instance, newValue);
    }
    
    /**
     * Sets a field value
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param field    The field
     * @param instance The field owner
     * @param newValue The new field value
     */
    public static void setFieldValue(Field field, Object instance, Object newValue) {
        exceptionHandler.runSafe(() -> field.set(instance, newValue));
    }
    
    /**
     * Sets static field value of the field name fieldName
     * To find the field, {@link #restrictedSearchField(Class, String)} is used (RESTRICTED search method).
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param clazz     The class to search for the field
     * @param fieldName the name of the field
     * @param newValue  The new field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #restrictedSearchField(Class, String)
     * @see #setStaticFieldValue(Field, Object)
     */
    public static void setStaticFieldValue(Class<?> clazz, String fieldName, Object newValue) {
        setStaticFieldValue(restrictedSearchField(clazz, fieldName), newValue);
    }
    
    /**
     * Sets static field value of the field named fieldName
     * To find the field, {@link #searchField(int, Class, String)} is used.
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param modifiers The modifiers for field search. Can have {@link #DEEP_SEARCH} and {@link #REMOVE_FINAL}
     * @param clazz     The class to search for the field
     * @param fieldName the name of the field
     * @param newValue  The new field value
     * @throws IllegalArgumentException if the field doesn't exist
     * @see #searchField(int, Class, String)
     * @see #setStaticFieldValue(Field, Object)
     */
    public static void setStaticFieldValue(int modifiers, Class<?> clazz, String fieldName, Object newValue) {
        setStaticFieldValue(searchField(modifiers, clazz, fieldName), newValue);
    }
    
    /**
     * Sets a static field value
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param field    The field
     * @param newValue The new field value
     */
    public static void setStaticFieldValue(Field field, Object newValue) {
        setFieldValue(field, (Object) null, newValue);
    }
    
    /*
    ### METHOD METHODS ###
     */
    
    /**
     * Search a method of any accessibility within the class or any of its superclasses.
     * The first method with the given name that is found will be returned.
     * <p>
     * If a method is found and it is not accessible, this method attempts to make it accessible.
     * If a {@link SecurityException} is thrown in the process, that is ignored and the method will be returned nonetheless.
     * <p>
     * This method throws IllegalArgumentException if the Method is not found, because, in most cases, that should never happen,
     * and it should simplify debugging. In some cases, if you want to know if the method exists, you'll have to use try/catch for that.
     *
     * @param clazz          The lowest class in the ladder to start searching from
     * @param methodName     The name of the method
     * @param parameterTypes the parameter types of the sought method.
     * @return The method
     * @throws NullPointerException     if clazz is null or methodName is null
     * @throws IllegalArgumentException if the method doesn't exist
     * @see #restrictedSearchMethod(Class, String, Class[])
     */
    public static Method deepSearchMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return deepSearchMethod(0, clazz, methodName, parameterTypes);
    }
    
    /**
     * Search a method of any accessibility within the class or any of its superclasses.
     * The first method with the given name that is found will be returned.
     * <p>
     * If a method is found and it is not accessible, this method attempts to make it accessible.
     * If a {@link SecurityException} is thrown in the process, that is ignored and the method will be returned nonetheless.
     * <p>
     * This method throws IllegalArgumentException if the Method is not found, because, in most cases, that should never happen,
     * and it should simplify debugging. In some cases, if you want to know if the method exists, you'll have to use try/catch for that.
     *
     * @param modifiers      The modifiers for method search. Can have {@link #IGNORE_PARAMS}
     * @param clazz          The lowest class in the ladder to start searching from
     * @param methodName     The name of the method
     * @param parameterTypes the parameter types of the sought method.
     * @return The method
     * @throws NullPointerException     if clazz is null or methodName is null
     * @throws IllegalArgumentException if the method doesn't exist
     * @see #restrictedSearchMethod(Class, String, Class[])
     */
    public static Method deepSearchMethod(int modifiers, Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Class<?> currentClass = clazz;
        Method result;
        do {
            // throws NPE if class or methodName is null
            result = internalSearchMethod(modifiers, currentClass, methodName, parameterTypes);
            if (result != null) {
                return result;
            }
            currentClass = currentClass.getSuperclass();
        } while (currentClass != null);
        
        throw new IllegalArgumentException("method not found in " + clazz.getCanonicalName() + " and superclasses: " + methodName);
    }
    
    /**
     * Search a method of any accessibility within the class, but not its superclasses.
     * <p>
     * If a method is found and it is not accessible, this method attempts to make it accessible.
     * If a {@link SecurityException} is thrown in the process, that is ignored and the method will be returned nonetheless.
     * <p>
     * This method throws IllegalArgumentException if the Method is not found, because, in most cases, that should never happen,
     * and it should simplify debugging. In some cases, if you want to know if the method exists, you'll have to use try/catch for that.
     *
     * @param clazz          The only class to search for the method
     * @param methodName     The name of the method
     * @param parameterTypes the parameter types of the sought method.
     * @return The method
     * @throws NullPointerException     if clazz or methodName is null
     * @throws IllegalArgumentException if the method does not exist
     */
    public static Method restrictedSearchMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return restrictedSearchMethod(0, clazz, methodName, parameterTypes);
    }
    
    /**
     * Search a method of any accessibility within the class, but not its superclasses.
     * <p>
     * If a method is found and it is not accessible, this method attempts to make it accessible.
     * If a {@link SecurityException} is thrown in the process, that is ignored and the method will be returned nonetheless.
     * <p>
     * This method throws IllegalArgumentException if the Method is not found, because, in most cases, that should never happen,
     * and it should simplify debugging. In some cases, if you want to know if the method exists, you'll have to use try/catch for that.
     *
     * @param modifiers      The modifiers for method search. Can have {@link #IGNORE_PARAMS}
     * @param clazz          The only class to search for the method
     * @param methodName     The name of the method
     * @param parameterTypes the parameter types of the sought method.
     * @return The method
     * @throws NullPointerException     if clazz or methodName is null
     * @throws IllegalArgumentException if the method does not exist
     */
    public static Method restrictedSearchMethod(int modifiers, Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Method result = internalSearchMethod(modifiers, clazz, methodName, parameterTypes);
        if (result == null) {
            throw new IllegalArgumentException("method not found in " + clazz.getCanonicalName() + ": " + methodName);
        }
        return result;
    }
    
    /**
     * Searches for a method using the given search method.
     * <p>
     * If a method is found and it is not accessible, this method attempts to make it accessible.
     * If a {@link SecurityException} is thrown in the process, that is ignored and the method will be returned nonetheless.
     * <p>
     * This method throws IllegalArgumentException if the Method is not found, because, in most cases, that should never happen,
     * and it should simplify debugging. In some cases, if you want to know if the method exists, you'll have to use try/catch for that.
     *
     * @param modifiers      The modifiers for method search. Can have {@link #DEEP_SEARCH} and {@link #IGNORE_PARAMS}
     * @param clazz          The class to search in/from
     * @param methodName     Name of the method
     * @param parameterTypes the parameter types of the sought method.
     * @return The method
     * @throws NullPointerException     if clazz or methodName is null
     * @throws IllegalArgumentException if the method is not found
     */
    public static Method searchMethod(int modifiers, Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if ((modifiers & DEEP_SEARCH) != 0) {
            return deepSearchMethod(modifiers, clazz, methodName, parameterTypes);
        } else {
            return restrictedSearchMethod(modifiers, clazz, methodName, parameterTypes);
        }
    }
    
    /**
     * @return The same as {@link #restrictedSearchMethod(Class, String, Class[]) }, but returns null instead of throwing IllegalArgumentException
     * @see #restrictedSearchMethod(Class, String, Class[])
     */
    private static Method internalSearchMethod(int modifiers, Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Method result = null;
        
        if ((modifiers & IGNORE_PARAMS) != 0) {
            
            // throws NullPointerException if either clazz or methodName are null.
            methodName = methodName.intern();
            for (Method method : clazz.getDeclaredMethods()) {
                // all method names are interned. Identity comparison is much faster.
                if (method.getName() == methodName) {
                    result = method;
                    break;
                }
            }
            
            if (result == null) {
                return null;
            }
            
        } else {
            
            try {
                // throws NullPointerException if either clazz or methodName are null.
                result = clazz.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException | SecurityException ex) {
                return null;
            }
            
        }
        
        if (!result.isAccessible()) try {
            result.setAccessible(true);
        } catch (SecurityException ignored) {
            
        }
        
        return result;
    }
    
    /**
     * Invokes the method named methodName with the given instance and arguments
     * To find the method, {@link #searchMethod(int, Class, String, Class[])} is used with no type parameters,
     * modifiers {@link #DEEP_SEARCH} and {@link #IGNORE_PARAMS}, and the class {@link Object#getClass() instance.getClass()}
     * <p>
     * To search the method with type parameters, you should search the method using {@link #searchMethod(int, Class, String, Class[])} or similar,
     * and call {@link #invokeMethod(Method, Object, Object...)}
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param methodName Name of the method
     * @param instance   The instance to invoke the method on
     * @param args       The arguments to use in the method call
     * @param <T>        The expected/known method return type
     * @return The result of calling the method
     * @throws NullPointerException     if instance or methodName is null
     * @throws IllegalArgumentException if the method is not found
     * @see #invokeMethod(Method, Object, Object...)
     */
    public static <T> T invokeMethod(Object instance, String methodName, Object... args) {
        return invokeMethod(searchMethod(DEEP_SEARCH | IGNORE_PARAMS, instance.getClass(), methodName), instance, args);
    }
    
    /**
     * Invokes the method named methodName with the given instance and arguments
     * To find the method, {@link #searchMethod(int, Class, String, Class[])} is used with no type parameters,
     * as well as the modifier {@link #IGNORE_PARAMS}
     * <p>
     * To search the method with type parameters, you should search the method using {@link #searchMethod(int, Class, String, Class[])} or similar,
     * and call {@link #invokeMethod(Method, Object, Object...)}
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param clazz      The class to search in/from
     * @param methodName Name of the method
     * @param instance   The instance to invoke the method on
     * @param args       The arguments to use in the method call
     * @param <T>        The expected/known method return type
     * @return The result of calling the method
     * @throws NullPointerException     if clazz or methodName is null
     * @throws IllegalArgumentException if the method is not found
     * @see #invokeMethod(Method, Object, Object...)
     */
    public static <T> T invokeMethod(Class<?> clazz, String methodName, Object instance, Object... args) {
        return invokeMethod(searchMethod(IGNORE_PARAMS, clazz, methodName), instance, args);
    }
    
    /**
     * Invokes the method named methodName with the given instance and arguments
     * To find the method, {@link #searchMethod(int, Class, String, Class[])} is used with no type parameters.
     * For this search, the result of calling {@link Object#getClass() instance.getClass()} is used.
     * <p>
     * To search the method with type parameters, you should search the method using {@link #searchMethod(int, Class, String, Class[])} or similar,
     * and call {@link #invokeMethod(Method, Object, Object...)}
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param modifiers  The modifiers for method search. Can have {@link #DEEP_SEARCH} and {@link #IGNORE_PARAMS}
     * @param methodName Name of the method
     * @param instance   The instance to invoke the method on
     * @param args       The arguments to use in the method call
     * @param <T>        The expected/known method return type
     * @return The result of calling the method
     * @throws NullPointerException     if instance or methodName is null
     * @throws IllegalArgumentException if the method is not found
     * @see #invokeMethod(Method, Object, Object...)
     */
    public static <T> T invokeMethod(int modifiers, Object instance, String methodName, Object... args) {
        return invokeMethod(searchMethod(modifiers, instance.getClass(), methodName), instance, args);
    }
    
    /**
     * Invokes the method named methodName with the given instance and arguments
     * To find the method, {@link #searchMethod(int, Class, String, Class[])} is used with no type parameters.
     * <p>
     * To search the method with type parameters, you should search the method using {@link #searchMethod(int, Class, String, Class[])} or similar,
     * and call {@link #invokeMethod(Method, Object, Object...)}
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param modifiers  The modifiers for method search. Can have {@link #DEEP_SEARCH} and {@link #IGNORE_PARAMS}
     * @param clazz      The class to search in/from
     * @param methodName Name of the method
     * @param instance   The instance to invoke the method on
     * @param args       The arguments to use in the method call
     * @param <T>        The expected/known method return type
     * @return The result of calling the method
     * @throws NullPointerException     if clazz or methodName is null
     * @throws IllegalArgumentException if the method is not found
     * @see #invokeMethod(Method, Object, Object...)
     */
    public static <T> T invokeMethod(int modifiers, Class<?> clazz, String methodName, Object instance, Object... args) {
        return invokeMethod(searchMethod(modifiers, clazz, methodName), instance, args);
    }
    
    /**
     * Invokes the method with the given instance and arguments
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param method   The method to invoke
     * @param instance The instance to invoke the method on
     * @param args     The arguments to use in the method call
     * @param <T>      The expected/known method return type
     * @return The result of calling the method
     */
    public static <T> T invokeMethod(Method method, Object instance, Object... args) {
        return exceptionHandler.supplySafe(() -> (T) method.invoke(instance, args));
    }
    
    /**
     * Invokes the static method named methodName with the given arguments
     * To find the method, {@link #searchMethod(int, Class, String, Class[])} is used with no type parameters,
     * as well as the modifier {@link #IGNORE_PARAMS}
     * <p>
     * To search the method with type parameters, you should search the method using {@link #searchMethod(int, Class, String, Class[])} or similar,
     * and call {@link #invokeMethod(Method, Object, Object...)}
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param clazz      The class to search in/from
     * @param methodName Name of the method
     * @param args       The arguments to use in the method call
     * @param <T>        The expected/known method return type
     * @return The result of calling the method
     * @throws NullPointerException     if clazz or methodName is null
     * @throws IllegalArgumentException if the method is not found
     * @see #invokeStaticMethod(Method, Object...)
     */
    public static <T> T invokeStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return invokeStaticMethod(searchMethod(IGNORE_PARAMS, clazz, methodName), args);
    }
    
    /**
     * Invokes the static method named methodName with the given arguments
     * To find the method, {@link #searchMethod(int, Class, String, Class[])} is used with no type parameters.
     * <p>
     * To search the method with type parameters, you should search the method using {@link #searchMethod(int, Class, String, Class[])} or similar,
     * and call {@link #invokeMethod(Method, Object, Object...)}
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param modifiers  The modifiers for method search. Can have {@link #DEEP_SEARCH} and {@link #IGNORE_PARAMS}
     * @param clazz      The class to search in/from
     * @param methodName Name of the method
     * @param args       The arguments to use in the method call
     * @param <T>        The expected/known method return type
     * @return The result of calling the method
     * @throws NullPointerException     if clazz or methodName is null
     * @throws IllegalArgumentException if the method is not found
     * @see #invokeStaticMethod(Method, Object...)
     */
    public static <T> T invokeStaticMethod(int modifiers, Class<?> clazz, String methodName, Object... args) {
        return invokeStaticMethod(searchMethod(modifiers, clazz, methodName), args);
    }
    
    /**
     * Invokes the static method with the given arguments
     * <p>
     * If a {@link ReflectiveOperationException} occurs, this is printed to {@link System#out}
     *
     * @param method The method to invoke
     * @param args   The arguments to use in the method call
     * @param <T>    The expected/known method return type
     * @return The result of calling the method
     * @see #invokeMethod(Method, Object, Object...)
     */
    public static <T> T invokeStaticMethod(Method method, Object... args) {
        return invokeMethod(method, (Object) null, args);
    }
    
}
