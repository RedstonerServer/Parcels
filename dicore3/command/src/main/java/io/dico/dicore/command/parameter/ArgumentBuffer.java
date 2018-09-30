package io.dico.dicore.command.parameter;

import io.dico.dicore.command.CommandException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Buffer for the arguments.
 * Easy to traverse for the parser.
 */
public class ArgumentBuffer extends AbstractList<String> implements Iterator<String>, RandomAccess {
    private String[] array;
    private int cursor = 0; // index of the next return value
    private transient ArgumentBuffer unaffectingCopy = null; // see #getUnaffectingCopy()

    public ArgumentBuffer(String label, String[] args) {
        this(combine(label, args));
    }

    private static String[] combine(String label, String[] args) {
        String[] result;
        //if (args.length > 0 && "".equals(args[args.length - 1])) {
        //    // drop the last element of args if it is empty
        //    result = args;
        //} else {
        result = new String[args.length + 1];
        //}
        System.arraycopy(args, 0, result, 1, result.length - 1);
        result[0] = Objects.requireNonNull(label);
        return result;
    }

    /**
     * Constructs a new ArgumentBuffer using the given array, without copying it first.
     * None of the array its elements should be empty.
     *
     * @param array the array
     * @throws NullPointerException if the array or any of its elements are null
     */
    public ArgumentBuffer(String[] array) {
        for (String elem : array) {
            if (elem == null) throw new NullPointerException("ArgumentBuffer array element");
        }
        this.array = array;

    }

    public int getCursor() {
        return cursor;
    }

    public @NotNull ArgumentBuffer setCursor(int cursor) {
        if (cursor <= 0) {
            cursor = 0;
        } else if (size() <= cursor) {
            cursor = size();
        }
        this.cursor = cursor;
        return this;
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public @NotNull String get(int index) {
        return array[index];
    }

    public int nextIndex() {
        return cursor;
    }

    public int previousIndex() {
        return cursor - 1;
    }

    public int remainingElements() {
        return size() - nextIndex() - 1;
    }

    @Override
    public boolean hasNext() {
        return nextIndex() < size();
    }

    public boolean hasPrevious() {
        return 0 <= previousIndex();
    }

    /**
     * Unlike conventional ListIterator implementations, this returns null if there is no next element
     *
     * @return the next value, or null
     */
    @Override
    public @Nullable String next() {
        return hasNext() ? get(cursor++) : null;
    }

    public @NotNull String requireNext(String parameterName) throws CommandException {
        String next = next();
        if (next == null) {
            throw CommandException.missingArgument(parameterName);
        }
        return next;
    }

    // useful for completion code
    public @NotNull String nextOrEmpty() {
        return hasNext() ? get(cursor++) : "";
    }

    /**
     * Unlike conventional ListIterator implementations, this returns null if there is no previous element
     *
     * @return the previous value, or null
     */
    public @Nullable String previous() {
        return hasPrevious() ? get(--cursor) : null;
    }

    public @Nullable String peekNext() {
        return hasNext() ? get(cursor) : null;
    }

    public @Nullable String peekPrevious() {
        return hasPrevious() ? get(cursor - 1) : null;
    }

    public @NotNull ArgumentBuffer advance() {
        return advance(1);
    }

    public @NotNull ArgumentBuffer advance(int amount) {
        cursor = Math.min(Math.max(0, cursor + amount), size());
        return this;
    }

    public @NotNull ArgumentBuffer rewind() {
        return rewind(1);
    }

    public @NotNull ArgumentBuffer rewind(int amount) {
        return advance(-amount);
    }

    @NotNull String[] getArray() {
        return array;
    }

    public @NotNull String[] getArrayFromCursor() {
        return getArrayFromIndex(cursor);
    }

    public @NotNull String[] getArrayFromIndex(int index) {
        return Arrays.copyOfRange(array, index, array.length);
    }

    public @NotNull String getRawInput() {
        return String.join(" ", array);
    }

    public @NotNull String[] toArray() {
        return array.clone();
    }

    @Override
    public @NotNull Iterator<String> iterator() {
        return this;
    }

    @Override
    public @NotNull ListIterator<String> listIterator() {
        return new ListIterator<String>() {
            @Override
            public boolean hasNext() {
                return ArgumentBuffer.this.hasNext();
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return ArgumentBuffer.this.next();
            }

            @Override
            public boolean hasPrevious() {
                return ArgumentBuffer.this.hasPrevious();
            }

            @Override
            public String previous() {
                if (!hasPrevious()) {
                    throw new NoSuchElementException();
                }
                return ArgumentBuffer.this.previous();
            }

            @Override
            public int nextIndex() {
                return ArgumentBuffer.this.nextIndex();
            }

            @Override
            public int previousIndex() {
                return ArgumentBuffer.this.previousIndex();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(String s) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void dropTrailingEmptyElements() {
        int removeCount = 0;
        String[] array = this.array;
        for (int i = array.length - 1; i >= 0; i--) {
            if ("".equals(array[i])) {
                removeCount++;
            }
        }

        if (removeCount > 0) {
            String[] newArray = new String[array.length - removeCount];
            System.arraycopy(array, 0, newArray, 0, newArray.length);
            this.array = newArray;

            if (cursor > newArray.length) {
                cursor = newArray.length;
            }
        }
    }

    /**
     * Preprocess this argument buffer with the given preprocessor
     *
     * @param preProcessor preprocessor
     * @return a new ArgumentBuffer with processed contents. Might be this buffer if nothing changed.
     */
    public @NotNull ArgumentBuffer preprocessArguments(IArgumentPreProcessor preProcessor) {
        return preProcessor.process(this, -1);
    }

    /**
     * Allows a piece of code to traverse this buffer without modifying its cursor.
     * After this method has been called for the first time on this instance, if this method
     * or the {@link #clone()} method are called, the operation carried out on the prior result has finished.
     * As such, the same instance might be returned again.
     *
     * @return A view of this buffer that doesn't affect this buffer's cursor.
     */
    public ArgumentBuffer getUnaffectingCopy() {
        // the copy doesn't alter the cursor of this ArgumentBuffer when moved, but traverses the same array reference.
        // there is only ever one copy of an ArgumentBuffer, the cursor of which is updated on every call to this method.

        ArgumentBuffer unaffectingCopy = this.unaffectingCopy;
        if (unaffectingCopy == null) {
            this.unaffectingCopy = unaffectingCopy = new ArgumentBuffer(array);
        }
        unaffectingCopy.cursor = this.cursor;
        return unaffectingCopy;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public @NotNull ArgumentBuffer clone() {
        ArgumentBuffer result = getUnaffectingCopy();
        this.unaffectingCopy = null;
        return result;
    }

    @Override
    public String toString() {
        return String.format("ArgumentBuffer(size = %d, cursor = %d)", size(), getCursor());
    }

}
