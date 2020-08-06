/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

/**
 *
 * @author Project2100
 */
public class Coprocessor1 {

	private final Register[] fpRegisters;

	// IMPL dependant
	//private final Register fir;
	//
	// FCC 7-1 (31-25) : FS (24) : FCC0 (23) : ...
	private final Register fcsr;

	// Following registers are actual aliases of fcsr
	/*, fexr, fenr,*/
	//
	// 0^(31-8) : FCC (7-0)
	//private final Register fccr;
	//
	/**
	 * @implnote FS initially set to zero
	 *
	 */
	Coprocessor1() {
		//TODO Optimize!
		fpRegisters = new Register[Descriptor.values().length];

		fcsr = new Register("fcsr", 0);

		for (int i = 0; i < fpRegisters.length; i++)
			if (fpRegisters[i] == null)
				fpRegisters[i] = new Register(Descriptor.values()[i].name(), 0);
	}

	boolean getFCC(int code) {
		if (code > 7) throw new MIPSException("Bad code received: " + code);

		return ((code == 0)
				? ((fcsr.value & 0x00800000) >> 23)
				: ((fcsr.value & (1 << (code + 24))) >> (code + 24)))
				== 1;

	}

	public static enum Descriptor {
		$f0, $f1, $f2, $f3, $f4, $f5, $f6, $f7,
		$f8, $f9, $f10, $f11, $f12, $f13, $f14, $f15,
		$f16, $f17, $f18, $f19, $f20, $f21, $f22, $f23,
		$f24, $f25, $f26, $f27, $f28, $f29, $f30, $f31;
        
	}
    
        
    public static Descriptor findByName(String name) {

        for (Descriptor desc : Descriptor.values()) {
            if (desc.name().equals(name)) return desc;
        }

        return null;
    }

}
