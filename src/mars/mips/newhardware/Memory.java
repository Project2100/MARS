/*
 * MIT License
 * 
 * Copyright (c) 2020 Andrea Proietto
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mars.mips.newhardware;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.logging.Level;
import mars.Main;
import mars.settings.BooleanSettings;
import mars.simulator.Exceptions;
import mars.util.Binary;

/**
 * Modern implementation of MARS' main memory.
 *
 * @implNote A single word (i.e. 4 bytes, see {@link Integer#BYTES}) is stored
 * as an {@code int} value; the whole memory is partitioned in blocks of 1024
 * (1Ki) words (4KiB) implemented as arrays of integers. Therefore, addressing
 * offset is defined in the 12 least significant bits
 * (log_2(array_length*word_bytes)), and it will range from 0x0000 to 0x0fff.
 * <p>
 * The memory's addressing space is expressed in 32 bits, thus the remaining 20
 * most significant bits (addressing_space-addressing _offset) define the block
 * index, spanning from 0x00000000 to 0x00100000 and amounting to roughly a
 * million of pages (1MiP).</p>
 *
 * <p>
 * Byte ordering is little-endian.</p>
 *
 * @author Project2100
 */
public class Memory {
    
    // AP200412
    static {assert (Integer.BYTES == 4);}
    
    // Using unsigned interpretation
    static int log2(int value) {
        assert(value > 0);
        int bc = 0;
        value--;
        while (value > 0) {
            value >>>= 1;
            bc++;
        } 
        return bc;
    }

    // Memory's basic structure constants
    // B = byte, W = word/integer , P = page
    /**
     * The number of bits needed to encode an index of any byte in an integer
     */
    public static final int WORD_BYTES_BITS;
    static {
        assert(Integer.BYTES > 1);
        WORD_BYTES_BITS = log2(Integer.BYTES);
    }
    
    public static final int BLOCK_OFFSET_BITS = log2(Memory.BLOCK_WORDS) + WORD_BYTES_BITS;
    public static final int BLOCK_INDEX_BITS = 32 - BLOCK_OFFSET_BITS;
    public static final int BLOCK_WORDS = 0x00000400; // 1KiW(or integers) (= 4KiB)
    public static final int BLOCK_OFFSET_MASK = (BLOCK_WORDS << WORD_BYTES_BITS) - 1;
    public static final int MEMORY_BLOCK_COUNT = 0x00100000; // 1MiP (4GiB(in 32bit)/BLOCK_SIZE)

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

    public Memory() {
        this(MIPSMachine.defaultConfig);
    }

    public Memory(MIPSMachine.Configuration config) {
        primaryMemory = new TreeMap<>();

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
     * FOR ASSEMBLING USE!
     * 
     * Loads the given bytes into memory, starting from the specified address onwards. No segmentation checks are performed.
     * 
     * @param data 
     * @param address
     */
    public void loadBytes(byte[] data, int address) {
        // AP200422: CAUTION - TODO: the aligned bytes must be OR-ed with the already present values
        
        // Copy the data into an int array that is aligned with the memory word structure
        //<editor-fold defaultstate="collapsed" desc="Array alignment">
        {
            int alignmentPrefixBytes = address & 0x3;
            int alignmentSuffixBytes = (address + data.length) & 0x3;
            if (alignmentPrefixBytes != 0 || alignmentSuffixBytes != 0) {
                byte newdata[] = new byte[data.length + alignmentPrefixBytes + alignmentSuffixBytes];
                Arrays.fill(newdata, (byte) 0);
                System.arraycopy(data, 0, newdata, alignmentPrefixBytes, data.length);
                data = newdata;
            }
        }

        // Transcode the data into an array of ints CAUTION: WATCH OUT FOR ENDIANNESS HERE
        IntBuffer intBuf = ByteBuffer.wrap(data).asIntBuffer();
        int[] dataAsInt = new int[intBuf.remaining()];
        intBuf.get(dataAsInt);

        //</editor-fold>
        
        
        //<editor-fold defaultstate="collapsed" desc="Data transcription">
        int DWI = 0;
        int BWI = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BITS;
        while (true) {
            
            int[] block = getBlock(address);
            
            for (; BWI < block.length && DWI < dataAsInt.length; BWI++, DWI++) {
                block[BWI] = dataAsInt[DWI];
            }
            
            if (DWI == dataAsInt.length) break;
            
            address += BLOCK_WORDS * Integer.BYTES;
            BWI = 0;
        }
        //</editor-fold>
        
    }
    
    /**
     * FOR ASSEMBLING USE!
     * 
     * Loads the given instruction into memory, in the specified address. No segmentation checks are performed.
     * 
     * @param instruction 
     * @param address
     */
    public void loadInstruction(int instruction, int address) {
        //AP200422: How does endianness interact with instruction encoding?
        
        int[] block = getBlock(address);
        int BWI = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BITS;
        block[BWI] = instruction;
        
    }

    /**
     * Given a memory address, returns the block containing it.
     * 
     * @param address
     * @return 
     */
    private int[] getBlock(int address) {
        
        // AP200412: No bound checks, memory uses whole int range
        // Get page from memmap - will return null if not allocated
        // Logical shift - pages will have nonnegative indices
        int blockIndex = address >>> BLOCK_OFFSET_BITS;
        int[] block = primaryMemory.get(blockIndex);

        // if null, then allocate
        if (block == null) {
            Main.logger.log(Level.INFO, "Allocating block no. {0}\n"
                    + "Blocks already allocated: {1}\n"
                    + "Free memory: {2} B",
                    new Object[] {
                        blockIndex,
                        primaryMemory.size(),
                        Runtime.getRuntime().freeMemory()
                    });
            primaryMemory.put(blockIndex, block = new int[BLOCK_WORDS]);
        }

        return block;
    }
    
    static final int[] MEM_DUMMY = new int[BLOCK_WORDS];
    static {
        Arrays.fill(MEM_DUMMY, 0);
    }

    /**
     * Given a memory address, returns a view of the memory block that contains
     * it
     *
     * @implnote This method effectively creates a 1kB copy of memory if a block
     * is instantiated
     *
     * @param address
     * @return 
     */
    public int[] getBlockView(int address) {
        
        // AP200412: No bound checks, memory uses whole int range
        // Get page from memmap - will return null if not allocated
        // Logical shift - pages will have nonnegative indices
        int blockIndex = address >>> BLOCK_OFFSET_BITS;
        int[] b = primaryMemory.get(blockIndex);
        return b != null ? Arrays.copyOf(b, BLOCK_WORDS) : MEM_DUMMY;
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

    /**
     * Reads a value from memory at the given address with the specified format.
     * If the format is smaller than the WORD format, the returned values are
     * zero-extended.
     *
     * @implnote Double words not supported
     *
     * @param address
     * @param type
     * @param inKMode
     * @return the value read from memory, zero-extended if necessary
     * @throws AddressErrorException if the address is not correctly aligned
     * according to the data format, or is not in a legal address space
     */
    public int read(int address, Boundary type, boolean inKMode) throws AddressErrorException {

        if (type == Boundary.DOUBLE_WORD)
            throw new UnsupportedOperationException("DWORDS not supported");

        // Validate address and compute memory coordinates
        checkAddress(address, type, inKMode);
        int[] block = getBlock(address);
        int wordIndex = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BITS;
        int byteShift = (address & 3) << 3;

        // MUTEX over block (should implement readers/writers, affects performance?)
        // I am actually just reading, do I really need to synchronize?
        int value;
        synchronized (block) {
            value = block[wordIndex];
        }
        
        value = (value >> byteShift) & ((int) type.tmask);
        
        Main.logger.log(Level.FINER,
                "{0} bytes ({1}) written to memory at address {2}",
                new Object[] {type.size,
                    Binary.intToHexString(value),
                    Binary.intToHexString(address)});
        // TODO Observers' notify call
//        notifyAnyObservers(AccessNotice.READ, address, length, value);
        
        return value;
    }

    /**
     * Writes the given value on memory to the specified address according to
     * the given format
     *
     * @implNote If the boundary is not a word, the highest-order bytes are
     * truncated accordingly
     * <p>
     * The following restrictions apply to the specified segments.
     * <ul>
     * <li>Text: Only if self modifying code is permitted</li>
     * <li>Kernel data: Only in kernel mode</li>
     * <li>Kernel Text: Previous conditions must be met</li>
     * </ul>
     *
     * @param machine
     * @param address Starting address of Memory address to be set.
     * @param value Value to be stored starting at that address.
     * @param type the data format
     * @return old value that was replaced by the set operation
     * @throws AddressErrorException if the specified address is not in a valid
     * space or is misaligned
     */
    int write(MIPSMachine machine, int address, int value, Boundary type) throws AddressErrorException {

        if (type == Boundary.DOUBLE_WORD)
            throw new UnsupportedOperationException("DWORDS not supported");
        
        // Verify address and compute memory coordinates
        checkAddress(address, type, machine.coprocessor0.isInKMode());
        int[] block = getBlock(address);
        int wordIndex = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BITS;
        int byteShift = (address & 3) << 3;

        // MUTEX over block (should implement readers/writers, affects performance?)
        // Swap procedure
        int val = (value & (int) type.tmask) << byteShift;
        int mask = ~((int) type.tmask << byteShift);
        int old;
        synchronized (block) {
            old = block[wordIndex];
            block[wordIndex] = (block[wordIndex] & mask) | val;
        }
        
        // Update LL/SC status accordingly - extract word-aligned address
        // TODO THIS shhould be put back ino the executor for backstepping reasons!!!
        machine.coprocessor0.trialSync(address);

        Main.logger.log(Level.FINER,
                "{0} bytes ({1}) written to memory at address {2}",
                new Object[] {type.size,
                    Binary.intToHexString(value),
                    Binary.intToHexString(address)});
        // TODO Observers' notify call
//        notifyAnyObservers(AccessNotice.WRITE, address, length, value);

        return (old >> byteShift) & ((int) type.tmask);
    }

    private void checkAddress(int address, Boundary type, boolean inKernelMode)
            throws AddressErrorException {

        // Out of range check
        if (Integer.compareUnsigned(address, kernelHighAddress) > 0)
            throw new AddressErrorException(
                    "Address is out of range",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);

        // Alignment check
        // note: alignment guarantees word/halfword is not out of block bounds
        // WARNING Segment bounds are word aligned, a double may lie across
        // two segments simultaneously (see stack pointer/limit)
        if ((address & type.mask) != 0) throw new AddressErrorException(
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
    // TODO Check endian correctness
    long writeDouble(int address, long value, boolean inKernelMode) throws AddressErrorException {

        checkAddress(address, Boundary.DOUBLE_WORD, inKernelMode);

        // Get the corresponding block and calculate word index
        int[] block = getBlock(address);
        int wordIndex = (address & BLOCK_OFFSET_MASK) >> WORD_BYTES_BITS;

        // WORD-SPECIFIC SECTION
        //------------------
        // word splitting
        int hi = (int) (value >> 8);
        int lo = (int) value;

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
     * (starts at Memory.dataSegmentBaseAddress).
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

    /**
     * Allocates heap space, rounded up to the next word boundary.
     * <p>
     * There is no recycling and no heap management! TODO related to a
     * syscall</p>
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

    public static enum Boundary {
        DOUBLE_WORD(8, 7, 0xFFFFFFFF_FFFFFFFFl),
        WORD(4, 3, 0xFFFFFFFF),
        HALF_WORD(2, 1, 0xFFFF),
        BYTE(1, 0, 0xFF);
        public final int size;
        public final int mask;
        public final long tmask;

        Boundary(int s, int m, long tm) {
            size = s;
            mask = m;
            tmask = tm;
        }
    }

    /**
     * Enumeration of MIPS memory bounds.
     *
     * @implNote Memory configurations are stored as arrays of ints, caution is
     * advised while changing this enum's value declaration order
     *
     * @author Andrea "Project2100" Proietto
     * @since Oct 15, 2015
     */
    public static enum Descriptor {

        /**
         * base address for (user) text segment: 0x00400000
         */
        TEXT_BASE_ADDRESS(".text base address"),
        /**
         * base address for (user) data segment: 0x10000000
         */
        DATA_SEGMENT_ADDRESS("data segment base address"), //DataSegmentBaseAddress
        /**
         * base address for .extern directive: 0x10000000
         */
        EXTERN_BASE_ADDRESS(".extern base address"),
        /**
         * base address for storing globals: 0x10008000
         */
        GLOBAL_POINTER("global pointer $gp"),
        /**
         * base address for storage of non-global static data in data segment:
         * 0x10010000 (from SPIM not MIPS)
         */
        DATA_BASE_ADDRESS(".data base address"),
        /**
         * base address for heap: 0x10040000 (I think from SPIM not MIPS)
         */
        HEAP_BASE_ADDRESS("heap base address"),
        /**
         * starting address for stack: 0x7fffeffc (this is from SPIM not MIPS)
         */
        STACK_POINTER("stack pointer $sp"),
        /**
         * base address for stack: 0x7ffffffc (this is mine - start of highest
         * word below kernel space)
         */
        STACK_BASE_ADDRESS("stack base address"),
        /**
         * highest address accessible in user (not kernel) mode: 0x7fffffff
         */
        USER_SPACE_HIGH_ADDRESS("user space high address"), //UserHighAddress
        /**
         * kernel boundary. Only OS can access this or higher address:
         * 0x80000000
         */
        KERNEL_SPACE_BASE_ADDRESS("kernel space base address"), //KernelBaseAddress
        /**
         * base address for kernel text segment: 0x80000000
         */
        KTEXT_BASE_ADDRESS(".ktext base address"), //KernelTextBaseAddress
        /**
         * starting address for exception handlers: 0x80000180
         */
        EXCEPTION_HANDLER_ADDRESS("exception handler address"),
        /**
         * base address for kernel data segment: 0x90000000
         */
        KDATA_BASE_ADDRESS(".kdata base address"), //KernelDataBaseAddress
        /**
         * starting address for memory mapped I/O: 0xffff0000 (-65536)
         */
        MMIO_BASE_ADDRESS("MMIO base address"), //MemoryMapBaseAddress
        /**
         * highest address accessible in kernel mode: 0xffffffff
         */
        KERNEL_SPACE_HIGH_ADDRESS("kernel space high address"), // KernelHighAddress
        DATA_SEGMENT_LIMIT_ADDRESS("data segment limit address"),
        TEXT_LIMIT_ADDRESS("text limit address"),
        KERNEL_DATA_SEGMENT_LIMIT_ADDRESS("kernel data segment limit address"),
        KERNEL_TEXT_LIMIT_ADDRESS("kernel text limit address"),
        STACK_LIMIT_ADDRESS("stack limit address"),
        MEMORY_MAP_LIMIT_ADDRESS("memory map limit address");
        public final String name;

        private Descriptor(String name) {
            this.name = name;
        }
    }

    public static void main(String[] args) throws AddressErrorException {
//        Main.logger.setLevel(Level.FINE);
//        Memory m = new Memory();
//
//        m.set(0x10014002, 0x7625, 2, true);
//        m.set(0x10014003, 234, 1, true);

        System.out.println("" + 255 + " - " + (byte) 255 + " - " + ((258 & 0x0000FF00) >> 8));

        int instruction = 0b100000_00010_00101_1000000000001000;

        System.out.println("" + MIPSMachine.rs(instruction)
                + " - " + MIPSMachine.rt(instruction)
                + " - " + Integer.toHexString(MIPSMachine.opcode(instruction) << 2)
                + " - " + ((short) instruction << 16 >> 16));

        System.out.println("" + (0xFF & ((byte) -1)));
    }
}
