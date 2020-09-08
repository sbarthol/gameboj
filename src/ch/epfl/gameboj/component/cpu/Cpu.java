package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import static ch.epfl.gameboj.Preconditions.*;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Opcode.Family;
import ch.epfl.gameboj.component.memory.Ram;

public final class Cpu implements Clocked, Component {

	private enum Reg implements Register {
		A, F, B, C, D, E, H, L
	}

	private enum Reg16 implements Register {
		AF, BC, DE, HL
	}

	private enum FlagSrc {
		V0, V1, ALU, CPU
	}

	public enum Interrupt implements Bit {
		VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
	}

	private final RegisterFile<Reg> registerFile = new RegisterFile<>(Reg.values());
	private int PC = 0, SP = 0, IE = 0, IF = 0;
	private long nextNonIdleCycle = 0;
	private boolean IME = false;

	private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.DIRECT);
	private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.PREFIXED);

	private Bus bus;
	private Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

	@Override
	public void cycle(long cycle) {

		if (nextNonIdleCycle == Long.MAX_VALUE && Integer.lowestOneBit(IE & IF) != 0) {
			nextNonIdleCycle = cycle;
		}

		if (cycle < nextNonIdleCycle) {
			return;
		}
		reallyCycle();
	}

	@Override
	public int read(int address) {

		checkBits16(address);

		if (address == AddressMap.REG_IF) {
			return IF;

		} else if (address == AddressMap.REG_IE) {
			return IE;

		} else if (address >= AddressMap.HIGH_RAM_START && address < AddressMap.HIGH_RAM_END) {
			return highRam.read(address - AddressMap.HIGH_RAM_START);
		}

		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {

		checkBits16(address);
		checkBits8(data);

		if (address == AddressMap.REG_IF) {
			IF = data;

		} else if (address == AddressMap.REG_IE) {
			IE = data;

		} else if (address >= AddressMap.HIGH_RAM_START && address < AddressMap.HIGH_RAM_END) {
			highRam.write(address - AddressMap.HIGH_RAM_START, data);
		}
	}

	@Override
	public void attachTo(Bus bus) {
		this.bus = bus;
		bus.attach(this);
	}

	/**
	 * Lève l'interruption donnée, c-à-d met à 1 le bit correspondant dans le
	 * registre IF
	 * 
	 * @param i
	 */
	public void requestInterrupt(Interrupt i) {

		IF = Bits.set(IF, i.index(), true);
	}

	/**
	 * Retourne un tableau contenant, dans l'ordre, la valeur des registres PC, SP,
	 * A, F, B, C, D, E, H et L
	 * 
	 * @return
	 */
	public int[] _testGetPcSpAFBCDEHL() {

		int values[] = { PC, SP, registerFile.get(Reg.A), registerFile.get(Reg.F), registerFile.get(Reg.B),
				registerFile.get(Reg.C), registerFile.get(Reg.D), registerFile.get(Reg.E), registerFile.get(Reg.H),
				registerFile.get(Reg.L) };

		return values;
	}

	private static Opcode[] buildOpcodeTable(Opcode.Kind k) {

		Opcode[] ans = new Opcode[256];

		for (Opcode o : Opcode.values()) {

			if (o.kind == k) {
				ans[o.encoding] = o;
			}
		}

		return ans;
	}

	private void reallyCycle() {

		int k = Integer.lowestOneBit(IE & IF);

		if (k == 0 || !IME) {
			dispatch(read8(PC));

		} else {
			IME = false;
			IF = Bits.set(IF, Integer.SIZE - Integer.numberOfLeadingZeros(k) - 1, false);
			push16(PC);
			PC = AddressMap.INTERRUPTS[Integer.SIZE - Integer.numberOfLeadingZeros(k) - 1];
			nextNonIdleCycle += 5;
		}

	}

	private void dispatch(int encoding) {

		Opcode opcode = (encoding == 0xCB) ? PREFIXED_OPCODE_TABLE[read8AfterOpcode()] : DIRECT_OPCODE_TABLE[encoding];

		int nextPC = Bits.clip(16, PC + opcode.totalBytes);

		if (opcode.family == Family.NOP) {

			PC = nextPC;
			nextNonIdleCycle += opcode.cycles;
			return;

		}

		switch (opcode.family) {

		case NOP: {
		}
			break;
		case LD_R8_HLR: {

			registerFile.set(extractReg(opcode, 3), read8AtHl());

		}
			break;
		case LD_A_HLRU: {

			registerFile.set(Reg.A, read8AtHl());
			setReg16(Reg16.HL, reg16(Reg16.HL) + extractHlIncrement(opcode));

		}
			break;
		case LD_A_N8R: {

			int n8 = read8AfterOpcode();
			registerFile.set(Reg.A, read8(Bits.clip(16, AddressMap.REGS_START + n8)));

		}
			break;
		case LD_A_CR: {

			registerFile.set(Reg.A, read8(AddressMap.REGS_START + registerFile.get(Reg.C)));

		}
			break;
		case LD_A_N16R: {

			int n16 = read16AfterOpcode();
			registerFile.set(Reg.A, this.read8(n16));

		}
			break;
		case LD_A_BCR: {

			registerFile.set(Reg.A, read8(reg16(Reg16.BC)));

		}
			break;
		case LD_A_DER: {

			registerFile.set(Reg.A, read8(reg16(Reg16.DE)));

		}
			break;
		case LD_R8_N8: {

			Reg r8 = this.extractReg(opcode, 3);
			int n8 = read8AfterOpcode();
			registerFile.set(r8, n8);

		}
			break;
		case LD_R16SP_N16: {

			Reg16 r16 = this.extractReg16(opcode);
			int n16 = this.read16AfterOpcode();
			this.setReg16SP(r16, n16);

		}
			break;
		case POP_R16: {

			Reg16 r16 = this.extractReg16(opcode);
			this.setReg16(r16, this.pop16());

		}
			break;
		case LD_HLR_R8: {

			Reg r8 = this.extractReg(opcode, 0);
			this.write8AtHl(registerFile.get(r8));

		}
			break;
		case LD_HLRU_A: {

			this.write8AtHl(registerFile.get(Reg.A));
			this.setReg16(Reg16.HL, this.reg16(Reg16.HL) + this.extractHlIncrement(opcode));

		}
			break;
		case LD_N8R_A: {

			int n8 = this.read8AfterOpcode();
			this.write8(AddressMap.REGS_START + n8, registerFile.get(Reg.A));

		}
			break;
		case LD_CR_A: {

			this.write8(AddressMap.REGS_START + registerFile.get(Reg.C), registerFile.get(Reg.A));

		}
			break;
		case LD_N16R_A: {

			int n16 = this.read16AfterOpcode();
			this.write8(n16, registerFile.get(Reg.A));

		}
			break;
		case LD_BCR_A: {

			this.write8(reg16(Reg16.BC), registerFile.get(Reg.A));

		}
			break;
		case LD_DER_A: {

			this.write8(reg16(Reg16.DE), registerFile.get(Reg.A));

		}
			break;
		case LD_HLR_N8: {

			int n8 = this.read8AfterOpcode();
			this.write8AtHl(n8);

		}
			break;
		case LD_N16R_SP: {

			int n16 = this.read16AfterOpcode();
			this.write16(n16, SP);

		}
			break;
		case LD_R8_R8: {

			Reg r = this.extractReg(opcode, 3);
			Reg s = this.extractReg(opcode, 0);
			registerFile.set(r, registerFile.get(s));

		}
			break;
		case LD_SP_HL: {

			SP = reg16(Reg16.HL);

		}
			break;
		case PUSH_R16: {

			Reg16 r16 = this.extractReg16(opcode);
			this.push16(reg16(r16));

		}
			break;

		// Add
		case ADD_A_R8: {

			boolean c = Bits.test(opcode.encoding, 3) && Bits.test(registerFile.get(Reg.F), Alu.Flag.C);

			Reg r8 = this.extractReg(opcode, 0);
			int vf = Alu.add(registerFile.get(Reg.A), registerFile.get(r8), c);

			setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case ADD_A_N8: {

			boolean c = Bits.test(opcode.encoding, 3) && Bits.test(registerFile.get(Reg.F), Alu.Flag.C);

			int n8 = this.read8AfterOpcode();
			int vf = Alu.add(registerFile.get(Reg.A), n8, c);
			setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case ADD_A_HLR: {

			boolean c = Bits.test(opcode.encoding, 3) && Bits.test(registerFile.get(Reg.F), Alu.Flag.C);

			int n8 = this.read8AtHl();
			int vf = Alu.add(registerFile.get(Reg.A), n8, c);
			setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case INC_R8: {

			Reg r = this.extractReg(opcode, 3);
			int vf = Alu.add(registerFile.get(r), 1);
			setRegFromAlu(r, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;

		case INC_HLR: {

			int vf = Alu.add(this.read8AtHl(), 1);
			this.write8AtHl(Alu.unpackValue(vf));
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;

		case INC_R16SP: {

			Reg16 r = this.extractReg16(opcode);
			int vf = (r == Reg16.AF) ? Alu.add16L(SP, 1) : Alu.add16L(reg16(r), 1);
			this.setReg16SP(r, Alu.unpackValue(vf));

		}
			break;

		case ADD_HL_R16SP: {

			Reg16 r = this.extractReg16(opcode);
			int vf = (r == Reg16.AF) ? Alu.add16H(SP, reg16(Reg16.HL)) : Alu.add16H(reg16(r), reg16(Reg16.HL));
			this.setReg16(Reg16.HL, Alu.unpackValue(vf));
			this.combineAluFlags(vf, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case LD_HLSP_S8: {

			int e = this.read8AfterOpcode();
			e = Bits.signExtend8(e);
			e = Bits.clip(16, e);

			int vf = Alu.add16L(SP, e);
			this.combineAluFlags(vf, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

			if (Bits.test(opcode.encoding, 4)) {
				this.setReg16(Reg16.HL, Alu.unpackValue(vf));
			} else {
				SP = Alu.unpackValue(vf);
			}

		}
			break;

		// Subtract
		case SUB_A_R8: {

			boolean c = Bits.test(opcode.encoding, 3) && Bits.test(registerFile.get(Reg.F), Alu.Flag.C);

			Reg r = this.extractReg(opcode, 0);
			int vf = Alu.sub(registerFile.get(Reg.A), registerFile.get(r), c);
			setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case SUB_A_N8: {

			boolean c = Bits.test(opcode.encoding, 3) && Bits.test(registerFile.get(Reg.F), Alu.Flag.C);

			int n8 = this.read8AfterOpcode();
			int vf = Alu.sub(registerFile.get(Reg.A), n8, c);
			setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case SUB_A_HLR: {

			boolean c = Bits.test(opcode.encoding, 3) && Bits.test(registerFile.get(Reg.F), Alu.Flag.C);

			int hl = this.read8AtHl();
			int vf = Alu.sub(registerFile.get(Reg.A), hl, c);
			setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case DEC_R8: {

			Reg r = this.extractReg(opcode, 3);
			int vf = Alu.sub(registerFile.get(r), 1);
			setRegFromAlu(r, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;

		case DEC_HLR: {

			int vf = Alu.sub(this.read8AtHl(), 1);
			this.write8AtHl(Alu.unpackValue(vf));
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;

		case CP_A_R8: {

			Reg r = this.extractReg(opcode, 0);
			int vf = Alu.sub(registerFile.get(Reg.A), registerFile.get(r));
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case CP_A_N8: {

			int n8 = this.read8AfterOpcode();
			int vf = Alu.sub(registerFile.get(Reg.A), n8);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case CP_A_HLR: {

			int hl = this.read8AtHl();
			int vf = Alu.sub(registerFile.get(Reg.A), hl);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		case DEC_R16SP: {

			Reg16 r = this.extractReg16(opcode);
			int vf = (r == Reg16.AF) ? Alu.add16L(SP, Bits.clip(16, -1)) : Alu.add16L(reg16(r), Bits.clip(16, -1));
			setReg16SP(r, Alu.unpackValue(vf));

		}
			break;

		// And, or, xor, complement
		case AND_A_N8: {

			int n8 = this.read8AfterOpcode();
			int vf = Alu.and(registerFile.get(Reg.A), n8);
			setRegFlags(Reg.A, vf);

		}
			break;

		case AND_A_R8: {

			Reg r = this.extractReg(opcode, 0);
			int vf = Alu.and(registerFile.get(Reg.A), registerFile.get(r));
			setRegFlags(Reg.A, vf);

		}
			break;

		case AND_A_HLR: {

			int vf = Alu.and(registerFile.get(Reg.A), this.read8AtHl());
			setRegFlags(Reg.A, vf);

		}
			break;

		case OR_A_R8: {

			Reg r = this.extractReg(opcode, 0);
			int vf = Alu.or(registerFile.get(Reg.A), registerFile.get(r));
			setRegFlags(Reg.A, vf);

		}
			break;

		case OR_A_N8: {

			int n8 = this.read8AfterOpcode();
			int vf = Alu.or(registerFile.get(Reg.A), n8);
			setRegFlags(Reg.A, vf);

		}
			break;

		case OR_A_HLR: {

			int vf = Alu.or(registerFile.get(Reg.A), this.read8AtHl());
			setRegFlags(Reg.A, vf);

		}
			break;

		case XOR_A_R8: {

			Reg r = this.extractReg(opcode, 0);
			int vf = Alu.xor(registerFile.get(Reg.A), registerFile.get(r));
			setRegFlags(Reg.A, vf);

		}
			break;

		case XOR_A_N8: {

			int n8 = this.read8AfterOpcode();
			int vf = Alu.xor(registerFile.get(Reg.A), n8);
			setRegFlags(Reg.A, vf);

		}
			break;

		case XOR_A_HLR: {

			int vf = Alu.xor(registerFile.get(Reg.A), this.read8AtHl());
			setRegFlags(Reg.A, vf);

		}
			break;

		case CPL: {

			int vf = Alu.xor(registerFile.get(Reg.A), Bits.clip(8, -1));
			this.setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1, FlagSrc.CPU);

		}
			break;

		// Rotate, shift
		case ROTCA: {

			Alu.RotDir r = extractDir(opcode);
			int vf = Alu.rotate(r, registerFile.get(Reg.A));
			this.setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);

		}
			break;

		case ROTA: {

			Alu.RotDir r = extractDir(opcode);
			int vf = Alu.rotate(r, registerFile.get(Reg.A), Bits.test(registerFile.get(Reg.F), Alu.Flag.C));
			this.setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);

		}
			break;

		case ROTC_R8: {

			Reg r8 = this.extractReg(opcode, 0);
			Alu.RotDir r = extractDir(opcode);
			int vf = Alu.rotate(r, registerFile.get(r8));
			this.setRegFlags(r8, vf);

		}
			break;

		case ROT_R8: {

			Reg r8 = this.extractReg(opcode, 0);
			Alu.RotDir r = extractDir(opcode);
			int vf = Alu.rotate(r, registerFile.get(r8), Bits.test(registerFile.get(Reg.F), Alu.Flag.C));
			this.setRegFlags(r8, vf);

		}
			break;

		case ROTC_HLR: {

			Alu.RotDir r = extractDir(opcode);
			int vf = Alu.rotate(r, this.read8AtHl());
			this.write8AtHlAndSetFlags(vf);

		}
			break;

		case ROT_HLR: {

			Alu.RotDir r = extractDir(opcode);
			int vf = Alu.rotate(r, this.read8AtHl(), Bits.test(registerFile.get(Reg.F), Alu.Flag.C));
			this.write8AtHlAndSetFlags(vf);

		}
			break;

		case SWAP_R8: {

			Reg r8 = this.extractReg(opcode, 0);
			int vf = Alu.swap(registerFile.get(r8));
			this.setRegFlags(r8, vf);

		}
			break;

		case SWAP_HLR: {

			int vf = Alu.swap(this.read8AtHl());
			this.write8AtHlAndSetFlags(vf);

		}
			break;

		case SLA_R8: {

			Reg r = this.extractReg(opcode, 0);

			int vf = Alu.shiftLeft(registerFile.get(r));
			this.setRegFlags(r, vf);

		}
			break;

		case SRA_R8: {

			Reg r = this.extractReg(opcode, 0);
			int vf = Alu.shiftRightA(registerFile.get(r));
			this.setRegFlags(r, vf);

		}
			break;

		case SRL_R8: {

			Reg r = this.extractReg(opcode, 0);
			int vf = Alu.shiftRightL(registerFile.get(r));
			this.setRegFlags(r, vf);

		}
			break;

		case SLA_HLR: {

			int hl = this.read8AtHl();
			int vf = Alu.shiftLeft(hl);
			write8AtHlAndSetFlags(vf);

		}
			break;

		case SRA_HLR: {

			int hl = this.read8AtHl();
			int vf = Alu.shiftRightA(hl);
			write8AtHlAndSetFlags(vf);

		}
			break;

		case SRL_HLR: {

			int hl = this.read8AtHl();
			int vf = Alu.shiftRightL(hl);
			write8AtHlAndSetFlags(vf);

		}
			break;

		// Bit test and set
		case BIT_U3_R8: {

			int b = this.extractBits(opcode);
			Reg r8 = this.extractReg(opcode, 0);

			this.combineAluFlags(Alu.testBit(registerFile.get(r8), b), FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1,
					FlagSrc.CPU);

		}
			break;

		case BIT_U3_HLR: {

			int b = this.extractBits(opcode);
			this.combineAluFlags(Alu.testBit(this.read8AtHl(), b), FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);

		}
			break;

		case CHG_U3_R8: {

			int b = this.extractBits(opcode);
			Reg r8 = this.extractReg(opcode, 0);
			registerFile.set(r8, Bits.set(registerFile.get(r8), b, Bits.test(opcode.encoding, 6)));

		}
			break;

		case CHG_U3_HLR: {

			int b = this.extractBits(opcode);
			this.write8AtHl(Bits.set(read8AtHl(), b, Bits.test(opcode.encoding, 6)));

		}
			break;

		// Misc. ALU
		case DAA: {

			boolean n = Bits.test(registerFile.get(Reg.F), Alu.Flag.N);
			boolean h = Bits.test(registerFile.get(Reg.F), Alu.Flag.H);
			boolean c = Bits.test(registerFile.get(Reg.F), Alu.Flag.C);

			int vf = Alu.bcdAdjust(registerFile.get(Reg.A), n, h, c);
			this.setRegFromAlu(Reg.A, vf);
			this.combineAluFlags(vf, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU);

		}
			break;

		case SCCF: {

			boolean c = !(Bits.test(registerFile.get(Reg.F), Alu.Flag.C) && Bits.test(opcode.encoding, 3));
			this.combineAluFlags(Bits.set(0, Alu.Flag.C.index(), c), FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);

		}
			break;

		// Jumps
		case JP_HL: {

			nextPC = reg16(Reg16.HL);

		}
			break;
		case JP_N16: {

			nextPC = read16AfterOpcode();

		}
			break;
		case JP_CC_N16: {

			if (extractCondition(opcode)) {
				nextPC = read16AfterOpcode();
				nextNonIdleCycle += opcode.additionalCycles;
			}

		}
			break;
		case JR_E8: {

			int e = this.read8AfterOpcode();
			e = Bits.signExtend8(e);
			nextPC = Bits.clip(16, nextPC + e);

		}
			break;
		case JR_CC_E8: {

			if (this.extractCondition(opcode)) {

				int e = this.read8AfterOpcode();
				e = Bits.signExtend8(e);
				nextPC = Bits.clip(16, nextPC + e);
				nextNonIdleCycle += opcode.additionalCycles;
			}

		}
			break;

		// Calls and returns
		case CALL_N16: {

			int n16 = this.read16AfterOpcode();
			this.push16(nextPC);
			nextPC = n16;

		}
			break;
		case CALL_CC_N16: {

			if (this.extractCondition(opcode)) {

				int n16 = this.read16AfterOpcode();
				this.push16(nextPC);
				nextPC = n16;
				nextNonIdleCycle += opcode.additionalCycles;
			}

		}
			break;
		case RST_U3: {

			int n3 = this.extractBits(opcode);
			push16(nextPC);
			nextPC = AddressMap.RESETS[n3];

		}
			break;
		case RET: {

			nextPC = pop16();

		}
			break;
		case RET_CC: {

			if (this.extractCondition(opcode)) {

				nextPC = pop16();
				nextNonIdleCycle += opcode.additionalCycles;
			}

		}
			break;

		// Interrupts
		case EDI: {

			IME = Bits.test(opcode.encoding, 3);

		}
			break;
		case RETI: {

			IME = true;
			nextPC = pop16();

		}
			break;

		// Misc control
		case HALT: {

			nextNonIdleCycle = Long.MAX_VALUE;

		}
			break;
		case STOP:
			throw new Error("STOP is not implemented");

		default: {
			throw new IllegalArgumentException();
		}
		}

		PC = nextPC;
		nextNonIdleCycle += opcode.cycles;
	}

	// Accès au bus

	private int read8(int address) {
		return bus.read(address);
	}

	private int read8AtHl() {
		return read8(reg16(Reg16.HL));
	}

	private int read8AfterOpcode() {
		return read8(Bits.clip(16, PC + 1));
	}

	private int read16(int address) {
		return Bits.make16(read8(Bits.clip(16, address + 1)), read8(address));
	}

	private int read16AfterOpcode() {
		return read16(Bits.clip(16, PC + 1));
	}

	private void write8(int address, int v) {
		bus.write(address, v);
	}

	private void write16(int address, int v) {
		bus.write(address, Bits.clip(8, v));
		bus.write(address + 1, Bits.extract(v, 8, 8));
	}

	private void write8AtHl(int v) {
		write8(reg16(Reg16.HL), v);
	}

	private void push16(int v) {
		SP = Bits.clip(16, SP - 2);
		write16(SP, v);
	}

	private int pop16() {

		int v = read16(SP);
		SP = Bits.clip(16, SP + 2);
		return v;
	}

	// Gestion des paires de registres

	private int reg16(Reg16 r) {

		switch (r) {
		case AF:
			return Bits.make16(registerFile.get(Reg.A), registerFile.get(Reg.F));
		case BC:
			return Bits.make16(registerFile.get(Reg.B), registerFile.get(Reg.C));
		case DE:
			return Bits.make16(registerFile.get(Reg.D), registerFile.get(Reg.E));
		case HL:
			return Bits.make16(registerFile.get(Reg.H), registerFile.get(Reg.L));
		}

		throw new IllegalArgumentException();
	}

	private void setReg16(Reg16 r, int newV) {

		switch (r) {
		case AF: {
			registerFile.set(Reg.A, Bits.extract(newV, 8, 8));
			registerFile.set(Reg.F, Bits.clip(8, newV&0xfff0));

		}
			break;
		case BC: {
			registerFile.set(Reg.B, Bits.extract(newV, 8, 8));
			registerFile.set(Reg.C, Bits.clip(8, newV));

		}
			break;
		case DE: {
			registerFile.set(Reg.D, Bits.extract(newV, 8, 8));
			registerFile.set(Reg.E, Bits.clip(8, newV));

		}
			break;
		case HL: {
			registerFile.set(Reg.H, Bits.extract(newV, 8, 8));
			registerFile.set(Reg.L, Bits.clip(8, newV));

		}
			break;
		}
	}

	private void setReg16SP(Reg16 r, int newV) {

		if (r == Reg16.AF) {
			SP = newV;
		} else {
			setReg16(r, newV);
		}
	}

	// Extraction de paramètres

	private int extractBits(Opcode opcode) {

		return Bits.extract(opcode.encoding, 3, 3);
	}

	private Alu.RotDir extractDir(Opcode opcode) {

		if (Bits.test(opcode.encoding, 3)) {
			return Alu.RotDir.RIGHT;
		}
		return Alu.RotDir.LEFT;
	}

	private Reg extractReg(Opcode opcode, int startBit) {

		Reg r[] = { Reg.B, Reg.C, Reg.D, Reg.E, Reg.H, Reg.L, null, Reg.A };
		return r[Bits.extract(opcode.encoding, startBit, 3)];
	}

	private static final Reg16 r[] = { Reg16.BC, Reg16.DE, Reg16.HL, Reg16.AF };
	private Reg16 extractReg16(Opcode opcode) {

		return r[Bits.extract(opcode.encoding, 4, 2)];
	}

	private int extractHlIncrement(Opcode opcode) {

		return Bits.test(opcode.encoding, 4) ? -1 : 1;

	}

	private boolean extractCondition(Opcode opcode) {

		int c = Bits.extract(opcode.encoding, 3, 2);

		switch (c) {
		case 0b00:
			return !Bits.test(registerFile.get(Reg.F), Alu.Flag.Z);
		case 0b01:
			return Bits.test(registerFile.get(Reg.F), Alu.Flag.Z);
		case 0b10:
			return !Bits.test(registerFile.get(Reg.F), Alu.Flag.C);
		case 0b11:
			return Bits.test(registerFile.get(Reg.F), Alu.Flag.C);
		}

		return false;
	}

	// Gestion des fanions

	private void setRegFromAlu(Reg r, int vf) {

		int v = Alu.unpackValue(vf);
		registerFile.set(r, v);
	}

	private void setFlags(int valueFlags) {

		registerFile.set(Reg.F, Alu.unpackFlags(valueFlags));
	}

	private void setRegFlags(Reg r, int vf) {

		setRegFromAlu(r, vf);
		setFlags(vf);
	}

	private void write8AtHlAndSetFlags(int vf) {

		write8AtHl(Alu.unpackValue(vf));
		setFlags(vf);
	}

	private void combineAluFlags(int vf, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {

		int alu = createFlagMask(FlagSrc.ALU, z, n, h, c);
		int cpu = createFlagMask(FlagSrc.CPU, z, n, h, c);
		int v1 = createFlagMask(FlagSrc.V1, z, n, h, c);

		this.setFlags((alu & vf) | (cpu & registerFile.get(Reg.F)) | v1);
	}

	private int createFlagMask(FlagSrc f, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {

		return Alu.maskZNHC(z == f, n == f, h == f, c == f);
	}

}
