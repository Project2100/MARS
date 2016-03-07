package mars.mips.newhardware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import mars.Main;
import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.instructions.Instruction;
import mars.settings.BooleanSettings;
import mars.simulator.Exceptions;
import mars.util.Binary;

/*
 Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

 Developed by Pete Sanderson (psanderson@otterbein.edu)
 and Kenneth Vollmar (kenvollmar@missouristate.edu)

 Permission is hereby granted, free of charge, to any person obtaining 
 a copy of this software and associated documentation files (the 
 "Software"), to deal in the Software without restriction, including 
 without limitation the rights to use, copy, modify, merge, publish, 
 distribute, sublicense, and/or sell copies of the Software, and to 
 permit persons to whom the Software is furnished to do so, subject 
 to the following conditions:

 The above copyright notice and this permission notice shall be 
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 (MIT license, http://www.opensource.org/licenses/mit-license.html)
 */
/**
 * Represents MIPS memory. Different segments are represented by different data
 * structures.
 *
 * @implNote
 *
 * <h3>Data Segments</h3>
 *
 * The data segment is allocated in blocks of 1024 ints (4096 bytes). Each block
 * is referenced by a "block table" entry, and the table has 1024 entries. The
 * capacity is thus 1024 entries * 4096 bytes = 4 MB. Should be enough to cover
 * most programs!! Beyond that it would go to an "indirect" block (similar to
 * Unix i-nodes), which is not implemented.
 * <p/>
 * Although this scheme is an array of arrays, it is relatively space-efficient
 * since only the table is created initially. A 4096-byte block is not allocated
 * until a value is written to an address within it. Thus most small programs
 * will use only 8K bytes of space (the table plus one block). The index into
 * both arrays is easily computed from the address; access time is constant.
 * <p/>
 * SPIM stores statically allocated data (following first .data directive)
 * starting at location 0x10010000. This is the first Data Segment word beyond
 * the reach of $gp used in conjunction with signed 16 bit immediate offset. $gp
 * has value 0x10008000 and with the signed 16 bit offset can reach from
 * 0x10008000 - 0xFFFF = 0x10000000 (Data Segment base) to 0x10008000 + 0x7FFF =
 * 0x1000FFFF (the byte preceding 0x10010000).
 * <p/>
 * Using my scheme, 0x10010000 falls at the beginning of the 17'th block --
 * table entry 16. SPIM uses a heap base address of 0x10040000 which is not part
 * of the MIPS specification. (I don't have a reference for that offhand...)
 * Using my scheme, 0x10040000 falls at the start of the 65'th block -- table
 * entry 64. That leaves (1024-64) * 4096 = 3,932,160 bytes of space available
 * without going indirect.
 *
 * <h3>Text Segments</h3>
 *
 * I use a similar scheme for storing instructions. MIPS text segment ranges
 * from 0x00400000 all the way to data segment (0x10000000) a range of about 250
 * MB! So I'll provide table of blocks with similar capacity. This differs from
 * data segment somewhat in that the block entries do not contain int's, but
 * instead contain references to ProgramStatement objects.
 *
* * <h3>Stack</h3>
 *
 * The stack is modeled similarly to the data segment. It cannot share the same
 * data structure because the stack base address is very large. To store it in
 * the same data structure would require implementation of indirect blocks,
 * which has not been realized. So the stack gets its own table of blocks using
 * the same dimensions and allocation scheme used for data segment.
 * <p/>
 * The other major difference is the stack grows DOWNWARD from its base address,
 * not upward. I.e., the stack base is the largest stack address. This turns the
 * whole scheme for translating memory address to block-offset on its head! The
 * simplest solution is to calculate relative address (offset from base) by
 * subtracting the desired address from the stack base address (rather than
 * subtracting base address from desired address). Thus as the address gets
 * smaller the offset gets larger. Everything else works the same, so it shares
 * some private helper methods with data segment algorithms.
 *
 * <h3>Memory-mapped I/O</h3>
 *
 * Memory mapped I/O is simulated with a separate table using the same structure
 * and logic as data segment. Memory is allocated in 4K byte blocks. But since
 * MMIO address range is limited to 0xffff0000 to 0xfffffffc, there are only 64K
 * bytes total. Thus there will be a maximum of 16 blocks, and I suspect never
 * more than one since only the first few addresses are typically used. The only
 * exception may be a rogue program generating such addresses in a loop. Note
 * that the MMIO addresses are interpreted by Java as negative numbers since it
 * does not have unsigned types. As long as the absolute address is correctly
 * translated into a table offset, this is of no concern.
 *
 * <h3>Byte Order</h3>
 *
 * This implementation is purely big-endian, MIPS can handle either one.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class Memory extends Observable {

    private static final int BLOCK_LENGTH_WORDS = 1024;  // allocated blocksize 1024 ints == 4K bytes
    private static final int BLOCK_TABLE_LENGTH = 1024; // Each entry of table points to a block.
    private static final int TEXT_BLOCK_LENGTH_WORDS = 1024;  // allocated blocksize 1024 ints == 4K bytes
    private static final int TEXT_BLOCK_TABLE_LENGTH = 1024; // Each entry of table points to a block.
    private static final int MMIO_TABLE_LENGTH = 16; // Each entry of table points to a 4K block.
    
    private int[][] dataBlockTable;
    private int[][] kernelDataBlockTable;
    private ProgramStatement[][] textBlockTable;
    private ProgramStatement[][] kernelTextBlockTable;
    
    private int[][] stackBlockTable;

    private int[][] memoryMapBlockTable;

    /**
     * MIPS word length in bytes.
     *
     * @implNote Much of the code is hardwired for 4 byte words. Refactoring
     * this is low priority.
     */
    public static final int WORD_LENGTH_BYTES = 4;
    /**
     * Current setting for endianness: true is for little-endian, false for
     * big-endian. Default is little-endian (true)
     * <p/>
     * Big-endian means lowest numbered byte is leftmost: [0][1][2][3]
     * <br/>
     * Little-endian means lowest numbered byte is rightmost: [3][2][1][0]
     */
    private boolean littleEndian = true;

    public static final int dataSegmentMaxSize, kernelDataSegmentMaxSize, stackMaxSize;
    public static final int kernelTextSegmentMaxSize, textSegmentMaxSize;
    public static final int memoryMapMaxSize;

    static {
        dataSegmentMaxSize = kernelDataSegmentMaxSize = stackMaxSize
                = BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * WORD_LENGTH_BYTES;
        textSegmentMaxSize = kernelTextSegmentMaxSize
                = TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * WORD_LENGTH_BYTES;
        memoryMapMaxSize
                = BLOCK_LENGTH_WORDS * MMIO_TABLE_LENGTH * WORD_LENGTH_BYTES;
    }

    //--------------------------------------------------------------------------
    // Address cache
    private int textSegBaseAddress;
    private int dataSegBaseAddress;
    private int heapBaseAddress;
    private int stackBaseAddress;
    private int kernelTextBaseAddress;
    private int kernelDataBaseAddress;
    private int memoryMapBaseAddress;
    private int kernelHighAddress;

    // Set "top" address boundary to go with each "base" address.  This determines permissable
    // address range for user program.  Currently limit is 4MB, or 1024 * 1024 * 4 bytes based
    // on the table structures described above (except memory mapped IO, limited to 64KB by range).
    private int dataSegmentLimitAddress;
    private int textLimitAddress;
    private int stackLimitAddress;
    private int kernelDataSegmentLimitAddress;
    private int kernelTextLimitAddress;
    private int memoryMapLimitAddress;

    private int heapAddress;

    // Memory will maintain a collection of observables.  Each one is associated
    // with a specific memory address or address range, and each will have at least
    // one observer registered with it.  When memory access is made, make sure only
    // observables associated with that address send notices to their observers.
    // This assures that observers are not bombarded with notices from memory
    // addresses they do not care about.
    //
    // Would like a tree-like implementation, but that is complicated by this fact:
    // key for insertion into the tree would be based on Comparable using both low 
    // and high end of address range, but retrieval from the tree has to be based
    // on target address being ANYWHERE IN THE RANGE (not an exact key match).
    //
    // This collection is synchronized in order to ensure thread safety
    final Collection<MemoryObservable> observables
            = Collections.synchronizedList(new ArrayList<MemoryObservable>());

    /**
     * Get the names of segments available for memory dump.
     *
     * @return array of Strings, each string is segment name (e.g. ".text",
     * ".data")
     */
    public static String[] getSegmentNames() {
        return new String[] {".text", ".data"};
    }

    /**
     * Return array with segment address bounds for specified segment.
     *
     * @param segment String with segment name (initially ".text" and ".data")
     * @return array of two Integer, the base and limit address for that
     * segment. Null if parameter name does not match a known segment name.
     */
    public int[] getSegmentBounds(String segment) {
        switch (segment) {
            case ".text":
                return new int[] {textSegBaseAddress,textLimitAddress};
            case ".data":
                return new int[] {dataSegBaseAddress+0x00010000,dataSegmentLimitAddress};
            default:
                return null;
        }
    }

    // Default constructor
    public Memory() {
        this(MIPSMachine.defaultConfig);
    }

    // Constructor which takes a memory configuration as argument
    Memory(MIPSMachine.Configuration config) {
        configure(config);
    }

    void configure(MIPSMachine.Configuration config) {
        textSegBaseAddress = config.getAddress(Descriptor.TEXT_BASE_ADDRESS);
        dataSegBaseAddress = config.getAddress(Descriptor.DATA_SEGMENT_ADDRESS);
        heapBaseAddress = config.getAddress(Descriptor.HEAP_BASE_ADDRESS);
        stackBaseAddress = config.getAddress(Descriptor.STACK_BASE_ADDRESS);
        kernelTextBaseAddress = config.getAddress(Descriptor.KTEXT_BASE_ADDRESS);
        kernelDataBaseAddress = config.getAddress(Descriptor.KDATA_BASE_ADDRESS);
        memoryMapBaseAddress = config.getAddress(Descriptor.MMIO_BASE_ADDRESS);
        kernelHighAddress = config.getAddress(Descriptor.KERNEL_SPACE_HIGH_ADDRESS);

        dataSegmentLimitAddress = Math.min(
                config.getAddress(Descriptor.DATA_SEGMENT_LIMIT_ADDRESS),
                dataSegBaseAddress + dataSegmentMaxSize);
        textLimitAddress = Math.min(
                config.getAddress(Descriptor.TEXT_LIMIT_ADDRESS),
                textSegBaseAddress + textSegmentMaxSize);
        kernelDataSegmentLimitAddress = Math.min(
                config.getAddress(Descriptor.KERNEL_DATA_SEGMENT_LIMIT_ADDRESS),
                kernelDataBaseAddress + kernelDataSegmentMaxSize);
        kernelTextLimitAddress = Math.min(
                config.getAddress(Descriptor.KERNEL_TEXT_LIMIT_ADDRESS),
                kernelTextBaseAddress + kernelTextSegmentMaxSize);
        stackLimitAddress = Math.max( //caution: address inversion
                config.getAddress(Descriptor.STACK_LIMIT_ADDRESS),
                stackBaseAddress - stackMaxSize);
        memoryMapLimitAddress = Math.min(
                config.getAddress(Descriptor.MEMORY_MAP_LIMIT_ADDRESS),
                memoryMapBaseAddress + memoryMapMaxSize);
        initialize();
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

    private void initialize() {
        heapAddress = heapBaseAddress;
        textBlockTable = new ProgramStatement[TEXT_BLOCK_TABLE_LENGTH][];
        dataBlockTable = new int[BLOCK_TABLE_LENGTH][]; // array of null int[] references
        kernelTextBlockTable = new ProgramStatement[TEXT_BLOCK_TABLE_LENGTH][];
        kernelDataBlockTable = new int[BLOCK_TABLE_LENGTH][];
        stackBlockTable = new int[BLOCK_TABLE_LENGTH][];
        memoryMapBlockTable = new int[MMIO_TABLE_LENGTH][];
        System.gc(); // call garbage collector on any Table memory just deallocated. 	  
    }

    /**
     * Returns the next available word-aligned heap address. There is no
     * recycling and no heap management! There is however nearly 4MB of heap
     * space available in Mars.
     *
     * @param numBytes Number of bytes requested. Should be multiple of 4,
     * otherwise next higher multiple of 4 allocated.
     * @return address of allocated heap storage.
     * @throws IllegalArgumentException if number of requested bytes is negative
     * or exceeds available heap storage
     */
    public int allocateBytesFromHeap(int numBytes) throws IllegalArgumentException {
        int result = heapAddress;
        if (numBytes < 0)
            throw new IllegalArgumentException("request (" + numBytes + ") is negative heap amount");
        int newHeapAddress = heapAddress + numBytes;
        if (newHeapAddress % 4 != 0)
            newHeapAddress = newHeapAddress + (4 - newHeapAddress % 4); // next higher multiple of 4
        if (newHeapAddress >= dataSegmentLimitAddress)
            throw new IllegalArgumentException("request (" + numBytes + ") exceeds available heap storage");
        heapAddress = newHeapAddress;
        return result;
    }

    /**
     * Set byte order to either LITTLE_ENDIAN or BIG_ENDIAN. Default is
     * LITTLE_ENDIAN.
     *
     * @param order either LITTLE_ENDIAN or BIG_ENDIAN
     */
    public void setByteOrder(boolean order) {
        littleEndian = order;
    }

    /**
     * Retrieve memory byte order. Default is LITTLE_ENDIAN (like PCs).
     *
     * @return either LITTLE_ENDIAN or BIG_ENDIAN
     */
    public boolean isLittleEndian() {
        return littleEndian;
    }

    /**
     * ************************* THE SETTER METHODS **************************
     */
    /**
     * Starting at the given address, write the given value over the given
     * number of bytes. This one does not check for word boundaries, and copies
     * one byte at a time. If length == 1, takes value from low order byte. If
     * 2, takes from low order half-word.
     *
     * @implNote Allocates memory blocks if necessary.
     *
     * @param address Starting address of Memory address to be set.
     * @param value Value to be stored starting at that address.
     * @param length Number of bytes to be written.
     * @return old value that was replaced by the configure operation
     * @throws mars.mips.hardware.AddressErrorException
     */
    public int set(int address, int value, int length) throws AddressErrorException {
        int oldValue = 0;
        if (Main.debug)
            System.out.println("memory[" + address + "] set to " + value + "(" + length + " bytes)");
        int relativeByteAddress;
        
        if (inDataSegment(address)) {
            // in data segment.  Will write one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - dataSegBaseAddress; // relative to data segment start, in bytes
            oldValue = storeBytesInTable(dataBlockTable, relativeByteAddress, length, value);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // in stack.  Handle similarly to data segment write, except relative byte
            // address calculated "backward" because stack addresses grow down from base.
            relativeByteAddress = stackBaseAddress - address;
            oldValue = storeBytesInTable(stackBlockTable, relativeByteAddress, length, value);
        }
        else if (inTextSegment(address))
            // Burch Mod (Jan 2013): replace throw with call to setStatement 
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting

            if (BooleanSettings.SELF_MODIFYING_CODE.isSet()) {
                ProgramStatement oldStatement = getStatementNoNotify(address);
                if (oldStatement != null)
                    oldValue = oldStatement.getBinaryStatement();
                setStatement(address, new ProgramStatement(value, address));
            }
            else throw new AddressErrorException(
                        "Cannot write directly to text segment!",
                        Exceptions.ADDRESS_EXCEPTION_STORE, address);
        else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
            // memory mapped I/O.
            relativeByteAddress = address - memoryMapBaseAddress;
            oldValue = storeBytesInTable(memoryMapBlockTable, relativeByteAddress, length, value);
        }
        else if (inKernelDataSegment(address)) {
            // in kernel data segment.  Will write one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - kernelDataBaseAddress; // relative to data segment start, in bytes
            oldValue = storeBytesInTable(kernelDataBlockTable, relativeByteAddress, length, value);
        }
        else if (inKernelTextSegment(address))
            // DEVELOPER: PLEASE USE setStatement() TO WRITE TO KERNEL TEXT SEGMENT...
            throw new AddressErrorException(
                    "DEVELOPER: You must use setStatement() to write to kernel text segment!",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        else
            // falls outside Mars addressing range
            throw new AddressErrorException("address out of range ",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        notifyAnyObservers(AccessNotice.WRITE, address, length, value);
        return oldValue;
    }

    /**
     * Starting at the given word address, write the given value over 4 bytes (a
     * word). It must be written as is, without adjusting for byte order (little
     * vs big endian). Address must be word-aligned.
     *
     * @param address Starting address of Memory address to be configure.
     * @param value Value to be stored starting at that address.
     * @return old value that was replaced by the configure operation.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int setRawWord(int address, int value) throws AddressErrorException {
        int relative, oldValue = 0;
        if (address % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException("store address not aligned on word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        if (inDataSegment(address)) {
            // in data segment
            relative = (address - dataSegBaseAddress) >> 2; // convert byte address to words
            oldValue = storeWordInTable(dataBlockTable, relative, value);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // in stack.  Handle similarly to data segment write, except relative 
            // address calculated "backward" because stack addresses grow down from base.
            relative = (stackBaseAddress - address) >> 2; // convert byte address to words
            oldValue = storeWordInTable(stackBlockTable, relative, value);
        }
        else if (inTextSegment(address))
            // Burch Mod (Jan 2013): replace throw with call to setStatement 
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (BooleanSettings.SELF_MODIFYING_CODE.isSet()) {
                ProgramStatement oldStatement = getStatementNoNotify(address);
                if (oldStatement != null)
                    oldValue = oldStatement.getBinaryStatement();
                setStatement(address, new ProgramStatement(value, address));
            }
            else
                throw new AddressErrorException(
                        "Cannot write directly to text segment!",
                        Exceptions.ADDRESS_EXCEPTION_STORE, address);
        else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
            // memory mapped I/O.
            relative = (address - memoryMapBaseAddress) >> 2; // convert byte address to word
            oldValue = storeWordInTable(memoryMapBlockTable, relative, value);
        }
        else if (inKernelDataSegment(address)) {
            // in data segment
            relative = (address - kernelDataBaseAddress) >> 2; // convert byte address to words
            oldValue = storeWordInTable(kernelDataBlockTable, relative, value);
        }
        else if (inKernelTextSegment(address))
            // DEVELOPER: PLEASE USE setStatement() TO WRITE TO KERNEL TEXT SEGMENT...
            throw new AddressErrorException(
                    "DEVELOPER: You must use setStatement() to write to kernel text segment!",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        else
            // falls outside Mars addressing range
            throw new AddressErrorException("store address out of range ",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        notifyAnyObservers(AccessNotice.WRITE, address, WORD_LENGTH_BYTES, value);
        if (Main.isBackSteppingEnabled())
            Main.program.getBackStepper().addMemoryRestoreRawWord(address, oldValue);
        return oldValue;
    }

    /**
     * Starting at the given word address, write the given value over 4 bytes (a
     * word). The address must be word-aligned.
     *
     * @param address Starting address of Memory address to be configure.
     * @param value Value to be stored starting at that address.
     * @return old value that was replaced by setWord operation.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int setWord(int address, int value) throws AddressErrorException {
        if (address % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException(
                    "store address not aligned on word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        return (Main.isBackSteppingEnabled())
                ? Main.program.getBackStepper().addMemoryRestoreWord(address, set(address, value, WORD_LENGTH_BYTES))
                : set(address, value, WORD_LENGTH_BYTES);
    }

    /**
     * Starting at the given halfword address, write the lower 16 bits of given
     * value into 2 bytes (a halfword).
     *
     * @param address Starting address of Memory address to be configure.
     * @param value Value to be stored starting at that address. Only low order
     * 16 bits used.
     * @return old value that was replaced by setHalf operation.
     * @throws AddressErrorException If address is not on halfword boundary.
     */
    public int setHalf(int address, int value) throws AddressErrorException {
        if (address % 2 != 0)
            throw new AddressErrorException("store address not aligned on halfword boundary ",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        return (Main.isBackSteppingEnabled())
                ? Main.program.getBackStepper().addMemoryRestoreHalf(address, set(address, value, 2))
                : set(address, value, 2);
    }

    /**
     * Writes low order 8 bits of given value into specified Memory byte.
     *
     * @param address Address of Memory byte to be configure.
     * @param value Value to be stored at that address. Only low order 8 bits
     * used.
     * @return old value that was replaced by setByte operation.
     * @throws mars.mips.hardware.AddressErrorException
     */
    public int setByte(int address, int value) throws AddressErrorException {
        return (Main.isBackSteppingEnabled())
                ? Main.program.getBackStepper().addMemoryRestoreByte(address, set(address, value, 1))
                : set(address, value, 1);
    }

    /**
     * Writes 64 bit double value starting at specified Memory address. Note
     * that high-order 32 bits are stored in higher (second) memory word
     * regardless of "endianness".
     *
     * @param address Starting address of Memory address to be configure.
     * @param value Value to be stored at that address.
     * @return old value that was replaced by setDouble operation.
     * @throws mars.mips.hardware.AddressErrorException
     */
    public double setDouble(int address, double value) throws AddressErrorException {
        int oldHighOrder, oldLowOrder;
        long longValue = Double.doubleToLongBits(value);
        oldHighOrder = set(address + 4, Binary.highOrderLongToInt(longValue), 4);
        oldLowOrder = set(address, Binary.lowOrderLongToInt(longValue), 4);
        return Double.longBitsToDouble(Binary.twoIntsToLong(oldHighOrder, oldLowOrder));
    }

    /**
     * Stores ProgramStatement in Text Segment.
     *
     * @param address Starting address of Memory address to be configure. Must
     * be word boundary.
     * @param statement Machine code to be stored starting at that address --
     * for simulation purposes, actually stores reference to ProgramStatement
     * instead of 32-bit machine code.
     * @throws AddressErrorException If address is not on word boundary or is
     * outside Text Segment.
     * @see ProgramStatement
     */
    public void setStatement(int address, ProgramStatement statement) throws AddressErrorException {
        if (address % 4 != 0 || !(inTextSegment(address) || inKernelTextSegment(address)))
            throw new AddressErrorException(
                    "store address to text segment out of range or not aligned to word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_STORE, address);
        if (Main.debug)
            System.out.println("memory[" + address + "] set to " + statement.getBinaryStatement());
        if (inTextSegment(address))
            storeProgramStatement(address, statement, textSegBaseAddress, textBlockTable);
        else
            storeProgramStatement(address, statement, kernelTextBaseAddress, kernelTextBlockTable);
    }

    /**
     * *************************** THE GETTER METHODS ************************
     */
    /**
     * Starting at the given word address, read the given number of bytes (max
     * 4). This one does not check for word boundaries, and copies one byte at a
     * time. If length == 1, puts value in low order byte. If 2, puts into low
     * order half-word.
     *
     * @param address Starting address of Memory address to be read.
     * @param length Number of bytes to be read.
     * @return Value stored starting at that address.
     * @throws mars.mips.hardware.AddressErrorException
     */
    public int get(int address, int length) throws AddressErrorException {
        return get(address, length, true);
    }

    // Does the real work, but includes option to NOT notify observers.
    private int get(int address, int length, boolean notify) throws AddressErrorException {
        int value = 0;
        int relativeByteAddress;
        if (inDataSegment(address)) {
            // in data segment.  Will read one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - dataSegBaseAddress; // relative to data segment start, in bytes
            value = fetchBytesFromTable(dataBlockTable, relativeByteAddress, length);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // in stack. Similar to data, except relative address computed "backward"
            relativeByteAddress = stackBaseAddress - address;
            value = fetchBytesFromTable(stackBlockTable, relativeByteAddress, length);
        }

        else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
            // memory mapped I/O.
            relativeByteAddress = address - memoryMapBaseAddress;
            value = fetchBytesFromTable(memoryMapBlockTable, relativeByteAddress, length);
        }
        else if (inTextSegment(address))
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement 
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (BooleanSettings.SELF_MODIFYING_CODE.isSet()) {
                ProgramStatement stmt = getStatementNoNotify(address);
                value = stmt == null ? 0 : stmt.getBinaryStatement();
            }
            else throw new AddressErrorException(
                        "Cannot read directly from text segment!",
                        Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        else if (inKernelDataSegment(address)) {
            // in kernel data segment.  Will read one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - kernelDataBaseAddress; // relative to data segment start, in bytes
            value = fetchBytesFromTable(kernelDataBlockTable, relativeByteAddress, length);
        }
        else if (inKernelTextSegment(address))
            // DEVELOPER: PLEASE USE getStatement() TO READ FROM KERNEL TEXT SEGMENT...
            throw new AddressErrorException(
                    "DEVELOPER: You must use getStatement() to read from kernel text segment!",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        else
            // falls outside Mars addressing range
            throw new AddressErrorException("address out of range ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        if (notify)
            notifyAnyObservers(AccessNotice.READ, address, length, value);
        return value;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int. It
     * transfers the 32 bit value "raw" as stored in memory, and does not adjust
     * for byte order (big or little endian). Address must be word-aligned.
     *
     * @implNote The logic here is repeated in getRawWordOrNull(). Logic is
     * simplified by having this method just call getRawWordOrNull() then return
     * either the int of its return value, or 0 if it returns null. Doing so
     * would be detrimental to simulation runtime performance, so I decided to
     * keep the duplicate logic.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     * @see Memory#getRawWordOrNull(int)
     */
    public int getRawWord(int address) throws AddressErrorException {
        int value = 0;
        int relative;
        if (address % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException("address for fetch not aligned on word boundary",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        if (inDataSegment(address)) {
            // in data segment
            relative = (address - dataSegBaseAddress) >> 2; // convert byte address to words
            value = fetchWordFromTable(dataBlockTable, relative);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // in stack. Similar to data, except relative address computed "backward"
            relative = (stackBaseAddress - address) >> 2; // convert byte address to words
            value = fetchWordFromTable(stackBlockTable, relative);
        }
        else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
            // memory mapped I/O.
            relative = (address - memoryMapBaseAddress) >> 2;
            value = fetchWordFromTable(memoryMapBlockTable, relative);
        }
        else if (inTextSegment(address))
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement 
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (BooleanSettings.SELF_MODIFYING_CODE.isSet()) {
                ProgramStatement stmt = getStatementNoNotify(address);
                value = stmt == null ? 0 : stmt.getBinaryStatement();
            }
            else
                throw new AddressErrorException(
                        "Cannot read directly from text segment!",
                        Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        else if (inKernelDataSegment(address)) {
            // in kernel data segment
            relative = (address - kernelDataBaseAddress) >> 2; // convert byte address to words
            value = fetchWordFromTable(kernelDataBlockTable, relative);
        }
        else if (inKernelTextSegment(address))
            // DEVELOPER: PLEASE USE getStatement() TO READ FROM KERNEL TEXT SEGMENT...
            throw new AddressErrorException(
                    "DEVELOPER: You must use getStatement() to read from kernel text segment!",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        else
            // falls outside Mars addressing range
            throw new AddressErrorException("address out of range ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        notifyAnyObservers(AccessNotice.READ, address, Memory.WORD_LENGTH_BYTES, value);
        return value;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int and
     * return Integer. It transfers the 32 bit value "raw" as stored in memory,
     * and does not adjust for byte order (big or little endian). Address must
     * be word-aligned.
     *
     * Returns null if reading from text segment and there is no instruction at
     * the requested address. Returns null if reading from data segment and this
     * is the first reference to the MARS 4K memory allocation block (i.e., an
     * array to hold the memory has not been allocated).
     *
     * This method was developed by Greg Giberling of UC Berkeley to support the
     * memory dump feature that he implemented in Fall 2007.
     *
     * @implNote Logic is duplicated from getRawWord() for performance reasons.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address as an
     * Integer. Conditions that cause return value null are described above.
     * @throws AddressErrorException If address is not on word boundary.
     * @see Memory#getRawWord(int)
     */
    public Integer getRawWordOrNull(int address) throws AddressErrorException {
        Integer value = null;
        int relative;
        if (address % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException("address for fetch not aligned on word boundary",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        if (inDataSegment(address)) {
            // in data segment
            relative = (address - dataSegBaseAddress) >> 2; // convert byte address to words
            value = fetchWordOrNullFromTable(dataBlockTable, relative);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // in stack. Similar to data, except relative address computed "backward"
            relative = (stackBaseAddress - address) >> 2; // convert byte address to words
            value = fetchWordOrNullFromTable(stackBlockTable, relative);
        }
        else if (inTextSegment(address) || inKernelTextSegment(address))
            try {
                value = (getStatementNoNotify(address) == null) ? null : getStatementNoNotify(address).getBinaryStatement();
            }
            catch (AddressErrorException aee) {
                value = null;
            }
        else if (inKernelDataSegment(address)) {
            // in kernel data segment
            relative = (address - kernelDataBaseAddress) >> 2; // convert byte address to words
            value = fetchWordOrNullFromTable(kernelDataBlockTable, relative);
        }
        else
            // falls outside Mars addressing range
            throw new AddressErrorException("address out of range ", Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        // Do not notify observers.  This read operation is initiated by the 
        // dump feature, not the executing MIPS program.
        return value;
    }

    /**
     * Look for first "null" memory value in an address range. For text segment
     * (binary code), this represents a word that does not contain an
     * instruction. Normally use this to find the end of the program. For data
     * segment, this represents the first block of simulated memory (block
     * length currently 4K words) that has not been referenced by an
     * assembled/executing program.
     *
     * @param baseAddress lowest MIPS address to be searched; the starting point
     * @param limitAddress highest MIPS address to be searched
     * @return lowest address within specified range that contains "null" value
     * as described above.
     * @throws AddressErrorException if the base address is not on a word
     * boundary
     */
    public int getAddressOfFirstNull(int baseAddress, int limitAddress) throws AddressErrorException {
        int address = baseAddress;
        for (; address < limitAddress; address += Memory.WORD_LENGTH_BYTES)
            if (getRawWordOrNull(address) == null)
                break;
        return address;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int. Does
     * not use "get()"; we can do it faster here knowing we're working only with
     * full words.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int getWord(int address) throws AddressErrorException {
        if (address % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException("fetch address not aligned on word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        return get(address, WORD_LENGTH_BYTES, true);
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int. Does
     * not use "get()"; we can do it faster here knowing we're working only with
     * full words. Observers are NOT notified.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int getWordNoNotify(int address) throws AddressErrorException {
        if (address % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException("fetch address not aligned on word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        return get(address, WORD_LENGTH_BYTES, false);
    }

    /**
     * Starting at the given word address, read a 2 byte word into lower 16 bits
     * of int.
     *
     * @param address Starting address of word to be read.
     * @return Halfword (2-byte value) stored starting at that address, stored
     * in lower 16 bits.
     * @throws AddressErrorException If address is not on halfword boundary.
     */
    public int getHalf(int address) throws AddressErrorException {
        if (address % 2 != 0)
            throw new AddressErrorException("fetch address not aligned on halfword boundary ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        return get(address, 2);
    }

    /**
     * Reads specified Memory byte into low order 8 bits of int.
     *
     * @param address Address of Memory byte to be read.
     * @return Value stored at that address. Only low order 8 bits used.
     * @throws mars.mips.hardware.AddressErrorException
     */
    public int getByte(int address) throws AddressErrorException {
        return get(address, 1);
    }

    /**
     * Gets ProgramStatement from Text Segment.
     *
     * @param address Starting address of Memory address to be read. Must be
     * word boundary.
     * @return reference to ProgramStatement object associated with that
     * address, or null if none.
     * @throws AddressErrorException If address is not on word boundary or is
     * outside Text Segment.
     * @see ProgramStatement
     */
    public ProgramStatement getStatement(int address) throws AddressErrorException {
        return getStatement(address, true);
    }

    /**
     * Gets ProgramStatement from Text Segment without notifying observers.
     *
     * @param address Starting address of Memory address to be read. Must be
     * word boundary.
     * @return reference to ProgramStatement object associated with that
     * address, or null if none.
     * @throws AddressErrorException If address is not on word boundary or is
     * outside Text Segment.
     * @see ProgramStatement
     */
    public ProgramStatement getStatementNoNotify(int address) throws AddressErrorException {
        return getStatement(address, false);
    }

    // Actual statement getter
    private ProgramStatement getStatement(int address, boolean notify) throws AddressErrorException {
        if (!wordAligned(address))
            throw new AddressErrorException(
                    "fetch address for text segment not aligned to word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        if (!BooleanSettings.SELF_MODIFYING_CODE.isSet()
                && !(inTextSegment(address) || inKernelTextSegment(address)))
            throw new AddressErrorException(
                    "fetch address for text segment out of range ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, address);
        if (inTextSegment(address))
            return readProgramStatement(address, textSegBaseAddress, textBlockTable, notify);
        else if (inKernelTextSegment(address))
            return readProgramStatement(address, kernelTextBaseAddress, kernelTextBlockTable, notify);
        else
            return new ProgramStatement(get(address, WORD_LENGTH_BYTES), address);
    }

    /**
     * ************************** THE UTILITIES ******************************
     */
    /**
     * Utility to determine if given address is word-aligned.
     *
     * @param address the address to check
     * @return true if address is word-aligned, false otherwise
     */
    public static boolean wordAligned(int address) {
        return (address % WORD_LENGTH_BYTES == 0);
    }

    /**
     * Utility to determine if given address is doubleword-aligned.
     *
     * @param address the address to check
     * @return true if address is doubleword-aligned, false otherwise
     */
    public static boolean doublewordAligned(int address) {
        return (address % (WORD_LENGTH_BYTES + WORD_LENGTH_BYTES) == 0);
    }

    /**
     * Utility method to align given address to next full word boundary, if not
     * already aligned.
     *
     * @param address a memory address (any int value is potentially valid)
     * @return address aligned to next word boundary (divisible by 4)
     */
    public static int alignToWordBoundary(int address) {
        if (!wordAligned(address))
            if (address > 0)
                address += (4 - (address % WORD_LENGTH_BYTES));
            else
                address -= (4 - (address % WORD_LENGTH_BYTES));
        return address;
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
        return address >= textSegBaseAddress && address < textLimitAddress;
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
        return address >= kernelTextBaseAddress && address < kernelTextLimitAddress;
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
        return address >= dataSegBaseAddress && address < dataSegmentLimitAddress;
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
        return address >= kernelDataBaseAddress && address < kernelDataSegmentLimitAddress;
    }

    /**
     * Handy little utility to find out if given address is in the Memory Map
     * area starts at Memory.memoryMapBaseAddress, range 0xffff0000 to
     * 0xffffffff.
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined memory map (MMIO)
     * area, false otherwise.
     */
    public boolean inMemoryMapSegment(int address) {
        return address >= memoryMapBaseAddress && address < kernelHighAddress;
    }

    ///////////////////////////////////////////////////////////////////////////
    //  ALL THE OBSERVABLE STUFF GOES HERE.  FOR COMPATIBILITY, Memory IS STILL 
    //  EXTENDING OBSERVABLE, BUT WILL NOT USE INHERITED METHODS.  WILL INSTEAD
    //  USE A COLLECTION OF MemoryObserver OBJECTS, EACH OF WHICH IS COMBINATION
    //  OF AN OBSERVER WITH AN ADDRESS RANGE.
    /**
     * Method to accept registration from observer for any memory address.
     * Overrides inherited method. Note to observers: this class delegates
     * Observable operations so notices will come from the delegate, not the
     * memory object.
     *
     * @param obs the observer
     */
    @Override
    public void addObserver(Observer obs) {
        try {  // split so start address always >= end address
            this.addObserver(obs, 0, 0x7ffffffc);
            this.addObserver(obs, 0x80000000, 0xfffffffc);
        }
        catch (AddressErrorException aee) {
            System.out.println("Internal Error in Memory.addObserver: " + aee);
        }
    }

    /**
     * Method to accept registration from observer for specific address. This
     * includes the memory word starting at the given address. Note to
     * observers: this class delegates Observable operations so notices will
     * come from the delegate, not the memory object.
     *
     * @param obs the observer
     * @param addr the memory address which must be on word boundary
     * @throws mars.mips.hardware.AddressErrorException
     */
    public void addObserver(Observer obs, int addr) throws AddressErrorException {
        this.addObserver(obs, addr, addr);
    }

    /**
     * Method to accept registration from observer for specific address range.
     * The last byte included in the address range is the last byte of the word
     * specified by the ending address. Note to observers: this class delegates
     * Observable operations so notices will come from the delegate, not the
     * memory object.
     *
     * @param obs the observer
     * @param startAddr the low end of memory address range, must be on word
     * boundary
     * @param endAddr the high end of memory address range, must be on word
     * boundary
     * @throws mars.mips.hardware.AddressErrorException
     */
    public void addObserver(Observer obs, int startAddr, int endAddr) throws AddressErrorException {
        if (startAddr % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException("address not aligned on word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, startAddr);
        if (endAddr != startAddr && endAddr % WORD_LENGTH_BYTES != 0)
            throw new AddressErrorException("address not aligned on word boundary ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, startAddr);
        // upper half of address space (above 0x7fffffff) has sign bit 1 thus is seen as
        // negative.
        if (startAddr >= 0 && endAddr < 0)
            throw new AddressErrorException("range cannot cross 0x8000000; please split it up",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, startAddr);
        if (endAddr < startAddr)
            throw new AddressErrorException("end address of range < start address of range ",
                    Exceptions.ADDRESS_EXCEPTION_LOAD, startAddr);
        observables.add(new MemoryObservable(obs, startAddr, endAddr));
    }

    /**
     * Return number of observers
     *
     * @return
     */
    @Override
    public int countObservers() {
        return observables.size();
    }

    /**
     * Remove specified memory observers
     *
     * @param obs Observer to be removed
     */
    @Override
    public void deleteObserver(Observer obs) {
        synchronized (observables) {
            Iterator<MemoryObservable> it = observables.iterator();
            while (it.hasNext())
                it.next().deleteObserver(obs);
        }
    }

    /**
     * Remove all memory observers
     */
    @Override
    public void deleteObservers() {
        observables.clear();
    }

    /**
     * Overridden to be unavailable. The notice that an Observer receives does
     * not come from the memory object itself, but instead from a delegate.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void notifyObservers() {
        throw new UnsupportedOperationException();
    }

    /**
     * Overridden to be unavailable. The notice that an Observer receives does
     * not come from the memory object itself, but instead from a delegate.
     *
     * @param obj
     * @throws UnsupportedOperationException
     */
    @Override
    public void notifyObservers(Object obj) {
        throw new UnsupportedOperationException();
    }

    /////////////////////////////////////////////////////////////////////////
    // Private class whose objects will represent an observable-observer pair 
    // for a given memory address or range.
    private class MemoryObservable extends Observable implements Comparable<MemoryObservable> {

        private final int lowAddress, highAddress;

        public MemoryObservable(Observer obs, int startAddr, int endAddr) {
            lowAddress = startAddr;
            highAddress = endAddr;
            this.addObserver(obs);
        }

        public boolean match(int address) {
            return (address >= lowAddress && address <= highAddress - 1 + WORD_LENGTH_BYTES);
        }

        public void notifyObserver(MemoryAccessNotice notice) {
            this.setChanged();
            this.notifyObservers(notice);
        }

        // Useful to have for future refactoring, if it actually becomes worthwhile to sort
        // these or put 'em in a tree (rather than sequential search through list).
        @Override
        public int compareTo(MemoryObservable obj) {
            if (this.lowAddress < obj.lowAddress || this.lowAddress == obj.lowAddress && this.highAddress < obj.highAddress)
                return -1;
            if (this.lowAddress > obj.lowAddress || this.lowAddress == obj.lowAddress && this.highAddress > obj.highAddress)
                return -1;
            return 0;  // they have to be equal at this point.
        }
    }

    /**
     * ******************************* THE HELPERS
     * ************************************
     */
    ////////////////////////////////////////////////////////////////////////////////
    //
    // Method to notify any observers of memory operation that has just occurred.
    //
    // The "|| Globals.getGui()==null" is a hack added 19 July 2012 DPS.  IF MIPS simulation
    // is from command mode, Globals.program is null but still want ability to observe.
    private void notifyAnyObservers(int type, int address, int length, int value) {
        if ((Main.program != null || Main.getGUI() == null) && this.observables.size() > 0) {
            Iterator<MemoryObservable> it = this.observables.iterator();
            MemoryObservable mo;
            while (it.hasNext()) {
                mo = it.next();
                if (mo.match(address))
                    mo.notifyObserver(new MemoryAccessNotice(type, address, /*length,*/ value));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    //
    // Helper method to store 1, 2 or 4 byte value in table that represents MIPS
    // memory. Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size.
    // Modified 29 Dec 2005 to return old value of replaced bytes.
    //
    private static final boolean STORE = true;
    private static final boolean FETCH = false;

    private int storeBytesInTable(int[][] blockTable,
            int relativeByteAddress, int length, int value) {
        return storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, value, STORE);
    }

    ////////////////////////////////////////////////////////////////////////////////
    //
    // Helper method to fetch 1, 2 or 4 byte value from table that represents MIPS
    // memory.  Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size.
    //	
    private int fetchBytesFromTable(int[][] blockTable, int relativeByteAddress, int length) {
        return storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, 0, FETCH);
    }

    ////////////////////////////////////////////////////////////////////////////////		
    //
    // The helper's helper.  Works for either storing or fetching, little or big endian. 
    // When storing/fetching bytes, most of the work is calculating the correct array element(s) 
    // and element byte(s).  This method performs either store or fetch, as directed by its 
    // client using STORE or FETCH in last arg.
    // Modified 29 Dec 2005 to return old value of replaced bytes, for STORE.
    //
    private synchronized int storeOrFetchBytesInTable(int[][] blockTable,
            int relativeByteAddress, int length, int value, boolean op) {
        int relativeWordAddress, block, offset, bytePositionInMemory, bytePositionInValue;
        int oldValue = 0; // for STORE, return old values of replaced bytes
        int loopStopper = 3 - length;
        // IF added DPS 22-Dec-2008. NOTE: has NOT been tested with Big-Endian.
        // Fix provided by Saul Spatz; comments that follow are his.
        // If address in stack segment is 4k + m, with 0 < m < 4, then the
        // relativeByteAddress we want is stackBaseAddress - 4k + m, but the
        // address actually passed in is stackBaseAddress - (4k + m), so we
        // need to add 2m.  Because of the change in sign, we get the
        // expression 4-delta below in place of m.
        if (blockTable == stackBlockTable) {
            int delta = relativeByteAddress % 4;
            if (delta != 0)
                relativeByteAddress += (4 - delta) << 1;
        }
        for (bytePositionInValue = 3; bytePositionInValue > loopStopper; bytePositionInValue--) {
            bytePositionInMemory = relativeByteAddress % 4;
            relativeWordAddress = relativeByteAddress >> 2;
            block = relativeWordAddress / BLOCK_LENGTH_WORDS;  // Block number
            offset = relativeWordAddress % BLOCK_LENGTH_WORDS; // Word within that block
            if (blockTable[block] == null)
                if (op == STORE)
                    blockTable[block] = new int[BLOCK_LENGTH_WORDS];
                else
                    return 0;
            if (littleEndian)
                bytePositionInMemory = 3 - bytePositionInMemory;
            if (op == STORE) {
                oldValue = replaceByte(blockTable[block][offset], bytePositionInMemory,
                        oldValue, bytePositionInValue);
                blockTable[block][offset] = replaceByte(value, bytePositionInValue,
                        blockTable[block][offset], bytePositionInMemory);
            }
            else// op == FETCH
                value = replaceByte(blockTable[block][offset], bytePositionInMemory,
                        value, bytePositionInValue);
            relativeByteAddress++;
        }
        return (op == STORE) ? oldValue : value;
    }

    ////////////////////////////////////////////////////////////////////////////////
    //
    // Helper method to store 4 byte value in table that represents MIPS memory.
    // Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size.  Assumes address is word aligned, no endian processing.
    // Modified 29 Dec 2005 to return overwritten value.
    private synchronized int storeWordInTable(int[][] blockTable, int relative, int value) {
        int block, offset, oldValue;
        block = relative / BLOCK_LENGTH_WORDS;
        offset = relative % BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null)
            // First time writing to this block, so allocate the space.
            blockTable[block] = new int[BLOCK_LENGTH_WORDS];
        oldValue = blockTable[block][offset];
        blockTable[block][offset] = value;
        return oldValue;
    }

    ////////////////////////////////////////////////////////////////////////////////
    //
    // Helper method to fetch 4 byte value from table that represents MIPS memory.
    // Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size.  Assumes word alignment, no endian processing.
    //
    private synchronized int fetchWordFromTable(int[][] blockTable, int relative) {
        int value;
        int block, offset;
        block = relative / BLOCK_LENGTH_WORDS;
        offset = relative % BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null)
            // first reference to an address in this block.  Assume initialized to 0.
            value = 0;
        else
            value = blockTable[block][offset];
        return value;
    }

    ////////////////////////////////////////////////////////////////////////////////
    //
    // Helper method to fetch 4 byte value from table that represents MIPS memory.
    // Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size.  Assumes word alignment, no endian processing.
    //
    // This differs from "fetchWordFromTable()" in that it returns an Integer and
    // returns null instead of 0 if the 4K table has not been allocated.  Developed
    // by Greg Gibeling of UC Berkeley, fall 2007.
    //
    private synchronized Integer fetchWordOrNullFromTable(int[][] blockTable, int relative) {
        int value;
        int block, offset;
        block = relative / BLOCK_LENGTH_WORDS;
        offset = relative % BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null)
            // first reference to an address in this block.  Assume initialized to 0.
            return null;
        else
            value = blockTable[block][offset];
        return value;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Returns result of substituting specified byte of source value into specified byte 
    // of destination value. Byte positions are 0-1-2-3, listed from most to least 
    // significant.  No endian issues.  This is a private helper method used by get() & configure().
    private int replaceByte(int sourceValue, int bytePosInSource, int destValue, int bytePosInDest) {
        return // Set source byte value into destination byte position; configure other 24 bits to 0's...
                ((sourceValue >> (24 - (bytePosInSource << 3)) & 0xFF)
                << (24 - (bytePosInDest << 3)))
                // and bitwise-OR it with...
                | // Set 8 bits in destination byte position to 0's, other 24 bits are unchanged.
                (destValue & ~(0xFF << (24 - (bytePosInDest << 3))));
    }

    ///////////////////////////////////////////////////////////////////////
    // Reverses byte sequence of given value.  Can use to convert between big and
    // little endian if needed.
    private int reverseBytes(int source) {
        return (source >> 24 & 0x000000FF)
                | (source >> 8 & 0x0000FF00)
                | (source << 8 & 0x00FF0000)
                | (source << 24);
    }

    ///////////////////////////////////////////////////////////////////////   	
    // Store a program statement at the given address.  Address has already been verified
    // as valid.  It may be either in user or kernel text segment, as specified by arguments.
    private void storeProgramStatement(int address, ProgramStatement statement,
            int baseAddress, ProgramStatement[][] blockTable) {
        int relative = (address - baseAddress) >> 2; // convert byte address to words
        int block = relative / BLOCK_LENGTH_WORDS;
        int offset = relative % BLOCK_LENGTH_WORDS;
        if (block < TEXT_BLOCK_TABLE_LENGTH) {
            if (blockTable[block] == null)
                // No instructions are stored in this block, so allocate the block.
                blockTable[block] = new ProgramStatement[BLOCK_LENGTH_WORDS];
            blockTable[block][offset] = statement;
        }
    }

    ///////////////////////////////////////////////////////////////////////   	
    // Read a program statement from the given address.  Address has already been verified
    // as valid.  It may be either in user or kernel text segment, as specified by arguments.  
    // Returns associated ProgramStatement or null if none. 
    // Last parameter controls whether or not observers will be notified.
    private ProgramStatement readProgramStatement(int address, int baseAddress, ProgramStatement[][] blockTable, boolean notify) {
        int relative = (address - baseAddress) >> 2; // convert byte address to words
        int block = relative / TEXT_BLOCK_LENGTH_WORDS;
        int offset = relative % TEXT_BLOCK_LENGTH_WORDS;
        if (block < TEXT_BLOCK_TABLE_LENGTH)
            if (blockTable[block] == null || blockTable[block][offset] == null) {
                // No instructions are stored in this block or offset.
                if (notify)
                    notifyAnyObservers(AccessNotice.READ, address, Instruction.INSTRUCTION_LENGTH, 0);
                return null;
            }
            else {
                if (notify)
                    notifyAnyObservers(AccessNotice.READ, address, Instruction.INSTRUCTION_LENGTH, blockTable[block][offset].getBinaryStatement());
                return blockTable[block][offset];
            }
        if (notify)
            notifyAnyObservers(AccessNotice.READ, address, Instruction.INSTRUCTION_LENGTH, 0);
        return null;
    }

    //--------------------------------------------------------------------------
    // Internal classes
    

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

}