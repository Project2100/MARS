/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import mars.Main;
import mars.assembler.SymbolTable;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.instructions.Instruction;
import mars.settings.BooleanSettings;

/**
 * @implSpec text & data segments and memory limit must be multiples of 8 to
 * ensure doubleword alignment consistency (and avoid segfaults)
 * PENDING - Restrictions on MMIO and uData subsegments?
 *
 * @author Project2100
 */
public class MIPSMachine {

    // Starting with MARS 3.7, the configuration can be changed.
    private static final HashSet<Configuration> configurations = new HashSet<>(3);
    // The default configuration is based on SPIM.
    public static final Configuration defaultConfig;

    static {
        configurations.add(defaultConfig = new Configuration("Default", "Default", Configuration.defaultConfigValues));
        configurations.add(new Configuration("CompactDataAtZero", "Compact, Data at Address 0", Configuration.dataCompactConfigValues));
        configurations.add(new Configuration("CompactTextAtZero", "Compact, Text at Address 0", Configuration.textCompactConfigValues));
    }

    public static ArrayList<Configuration> getConfigurations() {
        return new ArrayList<>(configurations);
    }

    public static Configuration getConfigByName(String name) {
        return configurations.stream()
                .filter(config -> name.equals(config.getID()))
                .findFirst().orElse(null);
    }

    private Configuration currentConfig;

    private final Register pc;
    public final Register hi, lo;
    private Registers registers;
    private Coprocessor0 coprocessor0;
    private Coprocessor1 coprocessor1;
    private Memory memory;

    public MIPSMachine() {
        this(defaultConfig);
    }

    public MIPSMachine(Configuration config) {
        memory = new Memory();
        registers = new Registers();

        coprocessor0 = new Coprocessor0();
        coprocessor1 = new Coprocessor1();

        this.pc = new Register("pc", config.getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS));
        this.hi = new Register("hi", 0);
        this.lo = new Register("lo", 0);

        configure(config);
    }

    /**
     * For returning the program counter's initial (reset) value. PENDING: what
     * should I actually return?
     *
     * @return The program counter's initial value
     */
    public int getInitialProgramCounter() {
        return currentConfig.getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS);
    }

    /**
     * For returning the program counters value.
     *
     * @return The program counters value as an int.
     */
    // PENDING notify?
    public int getProgramCounter() {
        return pc.get();
    }

    /**
     * Method to increment the Program counter in the general case (not a jump
     * or branch).
     */
    //PENDING notify?
    public void incrementPC() {
        pc.set(pc.value + Instruction.INSTRUCTION_LENGTH);
    }

    /**
     * For setting the Program Counter. Note that ordinary PC update should be
     * done using incrementPC() method. Use this only when processing jumps and
     * branches.
     *
     * @param value The value to set the Program Counter to.
     * @return previous PC value
     */
    public int setProgramCounter(int value) {
        return Main.isBackSteppingEnabled()
                ? Main.program.getBackStepper().addPCRestore(pc.set(value))
                : pc.set(value);
    }

    /**
     * For initializing the Program Counter. Do not use this to implement jumps
     * and branches, as it will NOT record a backstep entry with the restore
     * value. If you need backstepping capability, use setProgramCounter
     * instead.
     *
     * @param value The value to set the Program Counter to.
     */
    // PENDING deleteme
    public void initializeProgramCounter(int value) {
        pc.set(value);
    }

    /**
     * Will initialize the Program Counter to either the default reset value, or
     * the address associated with source program global label "main", if it
     * exists as a text segment label and the global setting is set.
     *
     * @param startAtMain If true, will set program counter to address of
     * statement labeled 'main' (or other defined start label) if defined. If
     * not defined, or if parameter false, will set program counter to default
     * reset value.
     */
    public void initializeProgramCounter(boolean startAtMain) {
        int mainAddr = Main.symbolTable.getAddress(SymbolTable.getStartLabel());
        pc.set(startAtMain && mainAddr != SymbolTable.NOT_FOUND
                && (memory.inTextSegment(mainAddr) || memory.inKernelTextSegment(mainAddr))
                ? mainAddr
                : currentConfig.getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS));
    }

    public Configuration getCurrentConfiguration() {
        return currentConfig;
    }

    /**
     * Sets current memory configuration for simulated MIPS. Configuration is
     * collection of memory segment addresses. e.g. text segment starting at
     * address 0x00400000. Configuration can be modified starting with MARS 3.7.
     *
     * Changing memory configuration will clear it
     *
     * @param config
     * @return {@code true} if configuration has been changed, {@code false}
     * otherwise
     */
    public final boolean configure(Configuration config) {
        if (config == null || config.equals(currentConfig))
            return false;
        Main.logger.log(Level.CONFIG, "Changing machine configuration from {0} to {1}",
                new Object[] {currentConfig.getName(), config.getName()});

        memory.configure(config);
        registers.configure(config);
        initializeProgramCounter(BooleanSettings.START_AT_MAIN.isSet());
        hi.value = lo.value = 0;
        //TODO Reset coprocessors

        return true;
    }

    /**
     * Models the memory configuration for the simulated MIPS machine.
     * "configuration" refers to the starting memory addresses for the various
     * memory segments. The default configuration is based on SPIM. Starting
     * with MARS 3.7, the configuration can be changed.
     *
     * @implSpec
     *
     * Base addresses listed here must be aligned on word boundary (i.e
     * multiples of 4), whereas limit addresses align on a word's last byte,
     * except the stack limit which is word aligned.
     *
     * @author Pete Sanderson
     * @version August 2009
     */
    public static class Configuration {

        // Default configuration comes from SPIM
        private static final int[] defaultConfigValues = {
            0x00400000, // .text Base Address
            0x10000000, // Data Segment base address
            0x10000000, // .extern Base Address
            0x10008000, // Global Pointer $gp)
            0x10010000, // .data base Address
            0x10040000, // heap base address
            0x7fffeffc, // stack pointer $sp (from SPIM not MIPS)
            0x7ffffffc, // stack base address
            0x7fffffff, // highest address in user space
            0x80000000, // lowest address in kernel space
            0x80000000, // .ktext base address
            0x80000180, // exception handler address
            0x90000000, // .kdata base address
            0xffff0000, // MMIO base address
            0xffffffff, // highest address in kernel (and memory)
            0x7fffffff, // data segment limit address
            0x0ffffffc, // text limit address
            0xfffeffff, // kernel data segment limit address
            0x8ffffffc, // kernel text limit address
            0x10040000, // stack limit address
            0xffffffff // memory map limit address
        };

        // Compact allows 16 bit addressing, data segment starts at 0
        private static final int[] dataCompactConfigValues = {
            0x00003000, // .text Base Address
            0x00000000, // Data Segment base address
            0x00001000, // .extern Base Address
            0x00001800, // Global Pointer $gp)
            0x00000000, // .data base Address
            0x00002000, // heap base address
            0x00002ffc, // stack pointer $sp 
            0x00002ffc, // stack base address
            0x00003fff, // highest address in user space
            0x00004000, // lowest address in kernel space
            0x00004000, // .ktext base address
            0x00004180, // exception handler address
            0x00005000, // .kdata base address
            0x00007f00, // MMIO base address
            0x00007fff, // highest address in kernel (and memory)
            0x00002fff, // data segment limit address
            0x00003ffc, // text limit address
            0x00007eff, // kernel data segment limit address
            0x00004ffc, // kernel text limit address
            0x00002000, // stack limit address
            0x00007fff // memory map limit address
        };

        // Compact allows 16 bit addressing, text segment starts at 0
        private static final int[] textCompactConfigValues = {
            0x00000000, // .text Base Address
            0x00001000, // Data Segment base address
            0x00001000, // .extern Base Address
            0x00001800, // Global Pointer $gp)
            0x00002000, // .data base Address
            0x00003000, // heap base address
            0x00003ffc, // stack pointer $sp 
            0x00003ffc, // stack base address
            0x00003fff, // highest address in user space
            0x00004000, // lowest address in kernel space
            0x00004000, // .ktext base address
            0x00004180, // exception handler address
            0x00005000, // .kdata base address
            0x00007f00, // MMIO base address
            0x00007fff, // highest address in kernel (and memory)
            0x00003fff, // data segment limit address
            0x00000ffc, // text limit address
            0x00007eff, // kernel data segment limit address
            0x00004ffc, // kernel text limit address
            0x00003000, // stack limit address
            0x00007fff // memory map limit address
        };

        // Identifier is used for saving setting; name is used for display
        private final String identifier;
        private final String name;
        private final int[] addresses;

        private Configuration(String id, String name, int[] values) {
            super();
            identifier = id;
            this.name = name;
            addresses = values;
        }

        public String getID() {
            return identifier;
        }

        public String getName() {
            return name;
        }

        public int[] getAddresses() {
            return Arrays.copyOf(addresses, addresses.length);
        }

        public int getAddress(Memory.Descriptor d) {
            return addresses[d.ordinal()];
        }
    }
}
