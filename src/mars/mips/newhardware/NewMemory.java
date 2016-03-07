/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

import java.util.TreeMap;
import java.util.logging.Level;
import mars.Main;
import mars.settings.BooleanSettings;
import mars.simulator.Exceptions;
import mars.util.Binary;

/**
 * Modern implementation of MARS' main memory.
 *
 * @implNote
 *
 * A single word (i.e. 4 bytes) is stored as an {@code int} value; the whole
 * memory is partitioned in blocks of 1024 words (4KiB) implemented as arrays of
 * integers. Therefore, addressing offset is defined in the 12 least significant
 * bits (log_2(array_length*word_bytes)), and it will range from 0x0000 to
 * 0x0fff.
 * <p/>
 * The memory's addressing space is expressed in 32 bits, thus the remaining 20
 * most significant bits (addressing_space-addressing _offset) define the block
 * index, spanning from 0x00000000 to 0x00100000.
 *
 * @author Project2100
 */
public class NewMemory {

    private final int WORD_BYTES_BIT_COUNT = 2; //log_2(4)
    private final int BLOCK_WORDS = 0x00000400; // 1024 integers = 4KiB
    private final int BLOCK_OFFSET_BITS = 10 + WORD_BYTES_BIT_COUNT; // log_2(1024)+wordBitCount
    private final int BLOCK_OFFSET_MASK = (BLOCK_WORDS << WORD_BYTES_BIT_COUNT) - 1;
    private final int MEMORY_BLOCK_COUNT = 0x00100000; // 1MiP (4GiB(32bit)/PAGE_SIZE)
    private final int BLOCK_INDEX_BITS = 32 - BLOCK_OFFSET_BITS;

    private boolean bigEndian;

    //--------------------------------------------------------------------------
    // Segment addresses cache
    private final int textBaseAddress;
    private final int textLimitAddress;

    private final int dataBaseAddress;
    private final int heapBaseAddress;
    private final int stackBaseAddress;//==dataSegLimit-4 by definition?
    private final int stackLimitAddress;//==heapBase by definition?
    private final int dataLimitAddress;

    private final int kTextBaseAddress;
    private final int kTextLimitAddress;

    private final int kDataBaseAddress;
    private final int kDataLimitAddress;

    private final int memoryMapBaseAddress;
    private final int memoryMapLimitAddress;

    private final int kernelHighAddress;

    private int heapAddress;
    private final TreeMap<Integer, int[]> primaryMemory;

    public NewMemory() {
        this(MIPSMachine.defaultConfig);
    }

    public NewMemory(MIPSMachine.Configuration config) {
        primaryMemory = new TreeMap<>();
        bigEndian = false;

        textBaseAddress = config.getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS);
        dataBaseAddress = config.getAddress(Memory.Descriptor.DATA_SEGMENT_ADDRESS);
        heapBaseAddress = config.getAddress(Memory.Descriptor.HEAP_BASE_ADDRESS);
        stackBaseAddress = config.getAddress(Memory.Descriptor.STACK_BASE_ADDRESS);
        kTextBaseAddress = config.getAddress(Memory.Descriptor.KTEXT_BASE_ADDRESS);
        kDataBaseAddress = config.getAddress(Memory.Descriptor.KDATA_BASE_ADDRESS);
        memoryMapBaseAddress = config.getAddress(Memory.Descriptor.MMIO_BASE_ADDRESS);
        kernelHighAddress = config.getAddress(Memory.Descriptor.KERNEL_SPACE_HIGH_ADDRESS);

        dataLimitAddress = config.getAddress(Memory.Descriptor.DATA_SEGMENT_LIMIT_ADDRESS);
        textLimitAddress = config.getAddress(Memory.Descriptor.TEXT_LIMIT_ADDRESS);
        kDataLimitAddress = config.getAddress(Memory.Descriptor.KERNEL_DATA_SEGMENT_LIMIT_ADDRESS);
        kTextLimitAddress = config.getAddress(Memory.Descriptor.KERNEL_TEXT_LIMIT_ADDRESS);
        stackLimitAddress = config.getAddress(Memory.Descriptor.STACK_LIMIT_ADDRESS);
        memoryMapLimitAddress = config.getAddress(Memory.Descriptor.MEMORY_MAP_LIMIT_ADDRESS);

        heapAddress = heapBaseAddress;
    }

    void reset() {
        primaryMemory.clear();
        heapAddress = heapBaseAddress;
        //TODO call gc?
    }

    /**
     * Allocates heap space, rounded up to the next word boundary.
     * <p/>
     * There is no recycling and no heap management!
     *
     * @param numBytes Number of bytes requested. Should be multiple of 4,
     * otherwise next higher multiple of 4 is allocated.
     * @return address of allocated heap storage.
     * @throws IllegalArgumentException if number of requested bytes is negative
     * or exceeds available heap storage
     */
    public int allocateBytesFromHeap(int numBytes) {
        if (numBytes < 0)
            throw new IllegalArgumentException("Cannot allocate a negative byte amount!");

        // Word alignment correction
        if ((numBytes & 3) != 0)
            numBytes = (numBytes + 4) & 0xFFFFFFFC;

        // Out of memory check
        int newHeapAddress = heapAddress + numBytes;
        if (newHeapAddress >= dataLimitAddress)
            throw new IllegalArgumentException(
                    "Request denied: out of heap space! (Requested: "
                    + numBytes + " - Remaining: "
                    + (dataLimitAddress - heapAddress));

        int result = heapAddress;
        heapAddress = newHeapAddress;
        return result;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    /**
     * Sets byte order in memory. Will clear current state.
     *
     * @param bigEndian
     */
    public void setBigEndian(boolean bigEndian) {
        this.bigEndian = bigEndian;
        reset();
    }

    private int[] getBlock(int blockIndex) {
        // Method is private and all calls ensure index validity,
        // but it never hurts to put a check...
        if (blockIndex < 0 || blockIndex >= MEMORY_BLOCK_COUNT)
            throw new IndexOutOfBoundsException(Integer.toString(blockIndex));

        // Get page from memmap - will return null if not allocated
        int[] block = primaryMemory.get(blockIndex);

        // if null, then allocate
        if (block == null) {
            Main.logger.log(Level.INFO, "Allocating block {0}\n"
                    + "Index size: {1}\n"
                    + "Free memory: {2}",
                    new Object[] {
                        blockIndex,
                        primaryMemory.size(),
                        Runtime.getRuntime().freeMemory()
                    });
            primaryMemory.put(blockIndex, block = new int[BLOCK_WORDS]);
        }

        return block;
    }

    /**
     * Determine whether the current memory configuration has a maximum address
     * that can be stored in 16 bits.
     *
     * @return true if maximum address can be stored in 16 bits or less, false
     * otherwise
     */
    public boolean usingCompactMemoryConfiguration() {
        return (kernelHighAddress & 0x00007fff) == kernelHighAddress;
    }

    private static boolean isAligned(int address, Boundary type) {
        // note: alignment guarantees word/halfword is not out of block bounds
        // WARNING Segment bounds are word aligned, a double may lie across
        // two segments simultaneously (see stack pointer/limit)
        return (address & type.mask) == 0;

    }

    long writeDouble(int address, long value, boolean bigEndian, boolean inKernelMode) throws AddressErrorException {

        checkAddress(address, Boundary.DOUBLE_WORD, inKernelMode);

        // Get the corresponding block and calculate word index
        int[] block = getBlock(address >> BLOCK_OFFSET_BITS);
        int wordIndex = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BIT_COUNT;

        // WORD-SPECIFIC SECTION
        //------------------
        // word splitting
        int hi = (int) (value >> 8);
        int lo = (int) value;

        // Applying endianness
        if (bigEndian) {
            hi = Integer.reverseBytes(hi);
            lo = Integer.reverseBytes(lo);
        }

        // MUTEX over block (should implement readers/writers, affects performance?)
        long oldlo, oldhi;
        synchronized (block) {
            oldlo = block[wordIndex];
            oldhi = block[wordIndex + 1];
            block[wordIndex] = lo;
            block[wordIndex + 1] = hi;
        }

        return (oldhi << 8) | oldlo;
    }

    int writeWord(int address, int value, boolean bigEndian, boolean inKernelMode) throws AddressErrorException {

        checkAddress(address, Boundary.WORD, inKernelMode);

        // Get the corresponding block and calculate word index
        int[] block = getBlock(address >> BLOCK_OFFSET_BITS);
        int wordIndex = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BIT_COUNT;

        // WORD-SPECIFIC SECTION
        //------------------
        // Applying endianness
        if (bigEndian)
            value = Integer.reverseBytes(value);

        // MUTEX over block (should implement readers/writers, affects performance?)
        int old;
        synchronized (block) {
            old = block[wordIndex];
            block[wordIndex] = value;
        }

        return old;
    }

    short writeHalfWord(int address, short value, boolean bigEndian, boolean inKernelMode) throws AddressErrorException {

        checkAddress(address, Boundary.HALF_WORD, inKernelMode);

        // Get the corresponding block and calculate word index
        int[] block = getBlock(address >> BLOCK_OFFSET_BITS);
        int wordIndex = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BIT_COUNT;

        // HALFWORD-SPECIFIC SECTION
        //------------------
        // Applying endianness
        if (bigEndian)
            value = Short.reverseBytes(value);

        // Calculating halfword offsets
        int shamt = (address & 3) << 3;

        // MUTEX over block (should implement readers/writers, affects performance?)
        // Swap procedure
        int val = Short.toUnsignedInt(value) << shamt;
        int mask = ~(0xFFFF << shamt);
        int old;
        synchronized (block) {
            old = block[wordIndex];
            block[wordIndex] = (block[wordIndex] & mask) | val;
        }

        return (short) (old >> shamt);
    }

    byte writeByte(int address, byte value, boolean inKernelMode) throws AddressErrorException {

        checkAddress(address, Boundary.BYTE, inKernelMode);

        // Get the corresponding block and calculate word index
        int[] block = getBlock(address >> BLOCK_OFFSET_BITS);
        int wordIndex = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BIT_COUNT;

        // BYTE-SPECIFIC SECTION
        //------------------
        // Calculating byte offset
        int shamt = (address & 3) << 3;

        // MUTEX over block (should implement readers/writers, affects performance?)
        // Swap procedure
        int val = Byte.toUnsignedInt(value) << shamt;
        int mask = ~(0xFF << shamt);
        int old;
        synchronized (block) {
            old = block[wordIndex];
            block[wordIndex] = (block[wordIndex] & mask) | val;
        }

        return (byte) (old >> shamt);
    }

    public enum Boundary {
        DOUBLE_WORD(8, 7),
        WORD(4, 3),
        HALF_WORD(2, 1),
        BYTE(1, 0);
        final int size;
        final int mask;

        Boundary(int s, int m) {
            size = s;
            mask = m;
        }
    }

    private void checkAddress(int address, Boundary type, boolean inKernelMode)
            throws AddressErrorException {

        // Out of range check
        if (Integer.compareUnsigned(address, kernelHighAddress) > 0)
            throw new AddressErrorException(
                    "Address is out of range",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        
        // Alignment check
        if (!isAligned(address, type))
            throw new AddressErrorException(
                    "Address not aligned on " + type.name() + " boundary",
                    Exceptions.ADDRESS_EXCEPTION_STORE,
                    address);

        // PENDING - if subsegbounds are not restricted, log warnings about
        // possible misalignments
        if (inKernelDataSegment(address) && !inKernelMode)
            throw new AddressErrorException(
                    "Cannot write in kData while in user mode",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        if (inKernelTextSegment(address))
            throw new AddressErrorException(
                    "Cannot write in kText",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        if (inTextSegment(address) && !BooleanSettings.SELF_MODIFYING_CODE.isSet())
            throw new AddressErrorException(
                    "Self modifying code is not enabled",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    public int set(int address, int value, int length, boolean inKernelMode) throws AddressErrorException {
        return set(address, value, length, inKernelMode, bigEndian);
    }

    /**
     * Starting at the given address, write the given value over the given
     * number of bytes. This one does not check for word boundaries, and copies
     * one byte at a time. If length == 1, takes value from low order byte. If
     * 2, takes from low order half-word, other lengths default to 4.
     *
     * @implNote
     *
     * The following restrictions apply to the specified segments.
     * <ul>
     * <li>Kernel Text: Denied.</li>
     * <li>Kernel data: Only in kernel mode</li>
     * <li>Text: Only if self modifying code is permitted</li>
     * </ul>
     *
     * @param address Starting address of Memory address to be set.
     * @param value Value to be stored starting at that address.
     * @param length the specified size of data to be written
     * @param inKernelMode true if in kernel mode, false otherwise
     * @param bigEndian true if value is to be written big endian
     * @return old value that was replaced by the set operation
     * @throws mars.mips.newhardware.AddressErrorException
     */
    public int set(int address, int value, int length, boolean inKernelMode, boolean bigEndian)
            throws AddressErrorException {

        // WARNING: if text segment is involved, update the ProgramStatement
        // list accordingly!!
        int oldValue;
        switch (length) {
            case 1:
                oldValue = writeByte(address, (byte) value, inKernelMode);
                break;
            case 2:
                oldValue = writeHalfWord(address, (short) value, bigEndian, inKernelMode);
                break;
            case 4:
                oldValue = writeWord(address, value, bigEndian, inKernelMode);
            default:
                throw new IllegalArgumentException(length == 8
                        ? "Double words are not supported in this method"
                        : "Invalid enum value received");
        }
        Main.logger.log(Level.FINE,
                "{0} bytes ({1}) written to memory at address {2}",
                new Object[] {length,
                    Binary.intToHexString(value),
                    Binary.intToHexString(address)});
        // Observers' notify call
//        notifyAnyObservers(AccessNotice.WRITE, address, length, value);
        return oldValue;
    }

    /**
     * Handy little utility to find out if given address is in MARS text segment
     * (starts at Memory.textBaseAddress). Note that MARS does not implement the
     * entire MIPS text segment space, but it does implement enough for hundreds
     * of thousands of lines of code.
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined text segment, false
     * otherwise.
     */
    public boolean inTextSegment(int address) {
        return Integer.compareUnsigned(address, textBaseAddress) >= 0
                && Integer.compareUnsigned(address, textLimitAddress) < 0;
    }

    /**
     * Handy little utility to find out if given address is in MARS kernel text
     * segment (starts at Memory.kernelTextBaseAddress).
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined kernel text segment,
     * false otherwise.
     */
    public boolean inKernelTextSegment(int address) {
        return Integer.compareUnsigned(address, kTextBaseAddress) >= 0
                && Integer.compareUnsigned(address, kTextLimitAddress) < 0;
    }

    /**
     * Handy little utility to find out if given address is in MARS data segment
     * (starts at Memory.dataSegmentBaseAddress). Note that MARS does not
     * implement the entire MIPS data segment space, but it does support at
     * least 4MB.
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined data segment, false
     * otherwise.
     */
    public boolean inDataSegment(int address) {
        return Integer.compareUnsigned(address, dataBaseAddress) >= 0
                && Integer.compareUnsigned(address, dataLimitAddress) < 0;
    }

    /**
     * Handy little utility to find out if given address is in MARS kernel data
     * segment (starts at Memory.kernelDataSegmentBaseAddress).
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined kernel data segment,
     * false otherwise.
     */
    public boolean inKernelDataSegment(int address) {
        return Integer.compareUnsigned(address, kDataBaseAddress) >= 0
                && Integer.compareUnsigned(address, kDataLimitAddress) < 0;
    }

    public static void main(String[] args) throws AddressErrorException {
        Main.logger.setLevel(Level.FINE);
        NewMemory m = new NewMemory();

        m.set(0x10014002, 0x7625, 2, true);
        m.set(0x10014003, 234, 1, true);
    }
}
