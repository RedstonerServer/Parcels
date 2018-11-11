package io.dico.dicore;

public interface BitModifier {
    
    long mask();
    
    long get(long x);
    
    long set(long x, long value);
    
    default int lowerBound() {
        return Long.numberOfTrailingZeros(mask());
    }
    
    default int upperBound() {
        return 64 - Long.numberOfLeadingZeros(mask());
    }
    
    default int bitCount() {
        return Long.bitCount(mask());
    }
    
    default boolean getBoolean(long x) {
        return get(x) == 1;
    }
    
    default long setBoolean(long x, boolean value) {
        return set(x, value ? 1 : 0);
    }
    
    default int getInt(long x) {
        return (int) (get(x) & 0xFFFFFFFFL);
    }
    
    default long setInt(long x, int value) {
        return set(x, value & 0xFFFFFFFFL);
    }
    
    default short getShort(long x) {
        return (short) (get(x) & 0xFFFFL);
    }
    
    default long setShort(long x, int value) {
        return set(x, value & 0xFFFFL);
    }
    
    default byte getByte(long x) {
        return (byte) (get(x) & 0xFFL);
    }
    
    default long setByte(long x, int value) {
        return set(x, value & 0xFFL);
    }
    
    final class OfSingle implements BitModifier {
        private final long mask;
        
        public OfSingle(int bit) {
            if (bit < 0 || bit >= 64) {
                throw new IndexOutOfBoundsException();
            }
            this.mask = 1L << bit;
        }
        
        @Override
        public int bitCount() {
            return 1;
        }
        
        @Override
        public long mask() {
            return mask;
        }
        
        public boolean getBoolean(long x) {
            return (x & mask) != 0;
        }
        
        public long setBoolean(long x, boolean value) {
            return value ? (x | mask) : (x & ~mask);
        }
        
        @Override
        public long get(long x) {
            return getBoolean(x) ? 1 : 0;
        }
        
        @Override
        public long set(long x, long value) {
            if (value < 0 || value > 1) {
                throw new IllegalArgumentException();
            }
            return setBoolean(x, value == 1);
        }
    }
    
    final class OfMultiple implements BitModifier {
        private final int lowerBound;
        private final int bitCount;
        private final long mask;
        
        public OfMultiple(int lowerBound, int bitCount) {
            int upperBound = lowerBound + bitCount;
            if (lowerBound < 0 || lowerBound >= 64 || upperBound < 1 || upperBound > 64 || upperBound < lowerBound) {
                throw new IndexOutOfBoundsException();
            }
            this.lowerBound = lowerBound;
            this.bitCount = bitCount;
            this.mask = (Long.MIN_VALUE >> (bitCount - 1)) >>> (64 - bitCount - lowerBound);
        }
        
        @Override
        public int lowerBound() {
            return lowerBound;
        }
        
        @Override
        public int bitCount() {
            return bitCount;
        }
        
        @Override
        public int upperBound() {
            return lowerBound + bitCount;
        }
        
        @Override
        public long mask() {
            return mask;
        }
        
        @Override
        public long get(long x) {
            return (x & mask) >>> lowerBound;
        }
        
        @Override
        public long set(long x, long value) {
            return (x & ~mask) | ((value << lowerBound) & mask);
        }
        
    }
    
    class Builder {
        int currentIndex;
    
        public int getCurrentIndex() {
            return currentIndex;
        }
        
        public int getRemaining() {
            return 64 - currentIndex;
        }
        
        public OfSingle single() {
            checkAvailable(1);
            return new OfSingle(currentIndex++);
        }
        
        public BitModifier size(int size) {
            if (size == 1) {
                return single();
            }
            checkAvailable(size);
            BitModifier result = new OfMultiple(currentIndex, size);
            currentIndex += size;
            return result;
        }
        
        public BitModifier remaining() {
            return size(getRemaining());
        }
        
        private void checkAvailable(int size) {
            if (size <= 0 || currentIndex + size > 64) {
                throw new IllegalStateException("Exceeding bit count of a long");
            }
        }
        
    }
    
}
