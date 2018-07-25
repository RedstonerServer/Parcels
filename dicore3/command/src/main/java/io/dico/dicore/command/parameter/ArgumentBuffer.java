package io.dico.dicore.command.parameter;

import io.dico.dicore.command.CommandException;

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
     */
    public ArgumentBuffer(String[] array) {
        this.array = Objects.requireNonNull(array);
    }

    public int getCursor() {
        return cursor;
    }

    public ArgumentBuffer setCursor(int cursor) {
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
    public String get(int index) {
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
    public String next() {
        return hasNext() ? get(cursor++) : null;
    }

    public String requireNext(String parameterName) throws CommandException {
        String next = next();
        if (next == null) {
            throw CommandException.missingArgument(parameterName);
        }
        return next;
    }

    // useful for completion code
    public String nextOrEmpty() {
        return hasNext() ? get(cursor++) : "";
    }

    /**
     * Unlike conventional ListIterator implementations, this returns null if there is no previous element
     *
     * @return the previous value, or null
     */
    public String previous() {
        return hasPrevious() ? get(--cursor) : null;
    }

    public String peekNext() {
        return hasNext() ? get(cursor) : null;
    }

    public String peekPrevious() {
        return hasPrevious() ? get(cursor - 1) : null;
    }

    public ArgumentBuffer advance() {
        return advance(1);
    }

    public ArgumentBuffer advance(int amount) {
        cursor = Math.min(Math.max(0, cursor + amount), size());
        return this;
    }

    public ArgumentBuffer rewind() {
        return rewind(1);
    }

    public ArgumentBuffer rewind(int amount) {
        return advance(-amount);
    }

    String[] getArray() {
        return array;
    }

    public String[] getArrayFromCursor() {
        return getArrayFromIndex(cursor);
    }

    public String[] getArrayFromIndex(int index) {
        return Arrays.copyOfRange(array, index, array.length);
    }

    public String getRawInput() {
        return String.join(" ", array);
    }

    public String[] toArray() {
        return array.clone();
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }

    @Override
    public ListIterator<String> listIterator() {
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

    public ArgumentBuffer preprocessArguments(IArgumentPreProcessor preProcessor) {
        String[] array = this.array;
        // processor shouldn't touch any items prior to the cursor
        if (array != (array = preProcessor.process(cursor, array))) {
            return new ArgumentBuffer(array).setCursor(cursor);
        }
        return this;
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
    public ArgumentBuffer clone() {
        ArgumentBuffer result = getUnaffectingCopy();
        this.unaffectingCopy = null;
        return result;
    }

}
