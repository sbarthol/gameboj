package ch.epfl.gameboj.component.cpu;

import static ch.epfl.test.TestRandomizer.newRandom;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Opcode.Kind;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

class Etape4 {

    private Bus bus;
    private Cpu cpu;
    private Ram memory;
    private int c,k;
    
    private enum Reg implements Register {
        B, C, D, E, H, L, X, A, F
    }
    
    private Reg registers[] = {Reg.B, Reg.C, Reg.D, Reg.E, Reg.H, Reg.L, Reg.A};
        
    private static final Opcode[] tab = buildOpcodeTable(Opcode.Kind.DIRECT);
    private static final Opcode[] tab2 = buildOpcodeTable(Opcode.Kind.PREFIXED);
    
    private static Opcode[] buildOpcodeTable(Opcode.Kind k) {

        Opcode[] ans = new Opcode[256];

        for (Opcode o: Opcode.values()) {

            if (o.kind == k) {
                ans[o.encoding] = o;
            }
        }

        return ans;
    }
    
    private void exec(Opcode o, int[] args) {
        
        if(o.kind == Kind.PREFIXED) {
            
            if(args.length != 0) throw new IllegalArgumentException("args.length != 0");
            
            bus.write(k, 0xcb);
            bus.write(k+1, o.encoding);
            
            cpu.cycle(c);
            
            c+=o.cycles;
            k+=o.totalBytes;
            
        }else {
            
            int a = o.totalBytes-1;
            
            if(args.length != a) throw new IllegalArgumentException("args.length != o.totalbytes - 1");
            
            bus.write(k, o.encoding);
            for(int i=0;i<args.length;i++) {
                bus.write(k+i+1, args[i]);
            }
                        
            cpu.cycle(c);
            
            c+=o.cycles;
            k+=o.totalBytes;
        }
    }
        
    private int[] test() {
        return cpu._testGetPcSpAFBCDEHL();
    }
    
    private int SP() {
        return test()[1];
    }
    
    private int A() {
        return test()[2];
    }
    
    private int F() {
        return test()[3];
    }
    
    private int B() {
        return test()[4];
    }
    
    private int C() {
        return test()[5];
    }
    
    private int D() {
        return test()[6];
    }
    
    private int E() {
        return test()[7];
    }
    
    private int H() {
        return test()[8];
    }
    
    private int L() {
        return test()[9];
    }
    
    private int HLR() {
        return bus.read(HL());
    }
    
    private int BC() {
        return Bits.make16(B(), C());
    }
    
    private int DE() {
        return Bits.make16(D(), E());
    }
    
    private int HL() {
        return Bits.make16(H(), L());
    }
    
    private int reg(Reg r) {
        
        switch(r) {
        case A: return A();
        case B: return B();
        case C: return C();
        case D: return D();
        case E: return E();
        case F: return F();
        case H: return H();
        case L: return L();
        default: return 0;
        }
    }
    
    private void init() {
        
        bus = new GameBoy(null).bus();
        
        memory = new Ram(1<<16);
        RamController controller = new RamController(memory, 0);
        controller.attachTo(bus);
        
        cpu = new Cpu();
        cpu.attachTo(bus);
        
        c=0;
        k=0;
    }
    
    private void LD_A_N8(int n8) {
        
        exec(tab[0x3e],new int[] {n8});
        assertEquals(A(),n8);
    }
    
    private void LD_B_N8(int n8) {
        
        exec(tab[0x06],new int[] {n8});
        assertEquals(B(),n8);
    }
    
    private void LD_C_N8(int n8) {
        
        exec(tab[0x0e],new int[] {n8});
        assertEquals(C(),n8);
    }
    
    private void LD_D_N8(int n8) {
        
        exec(tab[0x16],new int[] {n8});
        assertEquals(D(),n8);
    }
    
    private void LD_E_N8(int n8) {
        
        exec(tab[0x1e],new int[] {n8});
        assertEquals(E(),n8);
    }
    
    private void LD_H_N8(int n8) {
        
        exec(tab[0x26],new int[] {n8});
        assertEquals(H(),n8);
    }
    
    private void LD_L_N8(int n8) {
        
        exec(tab[0x2e],new int[] {n8});
        assertEquals(L(),n8);
    }
    
    private void LD_BC_N16(int n16) {
        
        exec(tab[0x01],new int[] {Bits.clip(8, n16),Bits.extract(n16, 8, 8)});
        assertEquals(BC(),n16);
    }
    
    private void ADD_A_N8(int n8) {
        
        int tmp = A();
        exec(tab[0xc6],new int[] {n8});
        assertEquals(Bits.clip(8, tmp+n8),A()); // oveflow
        
        int f = Alu.unpackFlags(Alu.add(tmp, n8));
        assertEquals(f,F());
    }
    
    private void ADD_A_B() {
        
        int a = A();
        int b = B();
        exec(tab[0x80],new int[] {});
        assertEquals(Bits.clip(8, a+b),A()); // overflow
        
        int f = Alu.unpackFlags(Alu.add(a, b));
        assertEquals(f,F());
    }
    
    private void ADD_A_HLR() {
        
        int a = A();
        int hl = HL();
        exec(tab[0x86],new int[] {});
        assertEquals(Bits.clip(8, a+bus.read(hl)),A()); // overflow
        
        int f = Alu.unpackFlags(Alu.add(a, bus.read(hl)));
        assertEquals(f,F());
        
    }
    
    private void ADC_A_N8(int n8) {
        
        int a = A();
        int f = F();
        int c = Bits.test(f, Alu.Flag.C) ? 1 : 0;
        exec(tab[0xce],new int[] {n8});
        assertEquals(Bits.clip(8, a+n8+c),A()); // oveflow
        
        f = Alu.unpackFlags(Alu.add(a, n8, Bits.test(f, Alu.Flag.C)));
        assertEquals(f,F());
    }
    
    private void ADC_A_B() {
        
        int a = A();
        int b = B();
        int f = F();
        int c = Bits.test(f, Alu.Flag.C) ? 1 : 0;
        exec(tab[0x88],new int[] {});
        assertEquals(Bits.clip(8, a+b+c),A()); // oveflow
        
        f = Alu.unpackFlags(Alu.add(a, b, Bits.test(f, Alu.Flag.C)));
        assertEquals(f,F());
    }
    
    private void ADC_A_HLR() {
        
        int a = A();
        int hl = bus.read(HL());
        int f = F();
        int c = Bits.test(f, Alu.Flag.C) ? 1 : 0;
        exec(tab[0x8e],new int[] {});
        assertEquals(Bits.clip(8, a+hl+c),A()); // oveflow
        
        f = Alu.unpackFlags(Alu.add(a, hl, Bits.test(f, Alu.Flag.C)));
        assertEquals(f,F());
    }
    
    private void INC_A() {
        
        int a = A();
        int f = F();
        exec(tab[0x3c],new int[] {});
        assertEquals(Bits.clip(8, a+1),A());
        
        int newF = Alu.unpackFlags(Alu.add(a, 1));
        assertEquals((newF&0b11100000)|(f&0b00010000),F());
    }
    
    private void INC_HLR() {
        
        int hl = bus.read(HL());
        int f = F();
        exec(tab[0x34],new int[] {});
        assertEquals(Bits.clip(8, hl+1),bus.read(HL()));
        
        int newF = Alu.unpackFlags(Alu.add(hl, 1));
        assertEquals((newF&0b11100000)|f&(0b00010000),F());
        
    }
    
    private void ADD_HL_BC() {
        
        int v=Bits.clip(16, HL()+BC());
        int f=Alu.unpackFlags(Alu.add16H(HL(), BC()));
        f=(f&0b0011_0000)|(F()&0b1000_0000);
        exec(tab[0x09],new int[] {});
        assertEquals(v,HL());
        assertEquals(f,F());
    }
    
    private void ADD_HL_SP() {
        
        int v=Bits.clip(16, HL()+SP());
        int f=Alu.unpackFlags(Alu.add16H(HL(), SP()));
        f=(f&0b00110000)|(F()&0b10000000);
        exec(tab[0x39],new int[] {});
        assertEquals(v,HL());
        assertEquals(f,F());
    }
    
    private void INC_DE() {
        
        int v=Bits.clip(16, DE()+1);
        int f=F();
        exec(tab[0x13],new int[] {});
        assertEquals(f,F());
        assertEquals(v,DE());
    }
    
    private void INC_SP() {
        
        int v=Bits.clip(16, SP()+1);
        int f=F();
        exec(tab[0x33],new int[] {});
        assertEquals(f,F());
        assertEquals(v,SP());
    }
    
    private void ADD_SP_N(int e8) {
        
        int v=Bits.clip(16, SP()+Bits.signExtend8(e8));
        int f=Alu.unpackFlags(Alu.add16L(SP(), e8));
        f=f&(0b00110000);
        exec(tab[0xe8],new int[] {e8});
        assertEquals(f,F());
        assertEquals(v,SP());
    }
    
    private void LD_HL_SP_N8(int e8) {
        
        int v=Bits.clip(16, SP()+Bits.signExtend8(e8));
        
        int f=Alu.unpackFlags(Alu.add16L(SP(), e8));
        f=f&(0b00110000);
        exec(tab[0xf8],new int[] {e8});
        assertEquals(f,F());
        assertEquals(v,HL());
    }
    
    private void SUB_A_N8(int n8) {
        
        int vf = Alu.sub(A(), n8, false);
        int f = Alu.unpackFlags(vf);
        exec(tab[0xd6],new int[] {n8});
        assertEquals(f,F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void SBC_A_HLR() {
        
        int vf = Alu.sub(A(), bus.read(HL()), Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        exec(tab[0x9e],new int[] {});
        assertEquals(f,F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void SBC_A_N8(int n8) {
        
        int vf = Alu.sub(A(), n8, Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        exec(tab[0xde],new int[] {n8});
        assertEquals(f,F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void SBC_A_B() {
        
        int vf = Alu.sub(A(), B(), Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        exec(tab[0x98],new int[] {});
        assertEquals(f,F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void DEC_A() {
        
        int vf = Alu.sub(A(), 1, false);
        int f = Alu.unpackFlags(vf);
        f=(f&0b1110_0000)|(F()&0b0001_0000);
        exec(tab[0x3d],new int[] {});
        assertEquals(f,F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void DEC_HLR() {
        
        int vf = Alu.sub(bus.read(HL()), 1, false);
        int f = Alu.unpackFlags(vf);
        f=(f&0b1110_0000)|(F()&0b0001_0000);
                
        exec(tab[0x35],new int[] {});
                
        assertEquals(Alu.unpackValue(vf),bus.read(HL()));
        assertEquals(f,F());
    }
    
    private void CP_A_N8(int n8) {
        
        int vf = Alu.sub(A(), n8, false);
        int f = Alu.unpackFlags(vf);
        exec(tab[0xfe],new int[] {n8});
        assertEquals(f,F());
    }
    
    private void CP_A_B() {
        
        int vf = Alu.sub(A(), B(), false);
        int f = Alu.unpackFlags(vf);
        exec(tab[0xb8],new int[] {});
        assertEquals(f,F());
    }
    
    private void CP_A_HLR() {
        
        int vf = Alu.sub(A(), bus.read(HL()), false);
        int f = Alu.unpackFlags(vf);
        exec(tab[0xbe],new int[] {});
        assertEquals(f,F());
    }
    
    private void DEC_SP() {
        
        int f = F();
        int vf = Alu.add16H(SP(), Bits.clip(16, -1));
        exec(tab[0x3b],new int[] {});
        assertEquals(f,F());
        assertEquals(SP(),Alu.unpackValue(vf));
    }
    
    private void AND_A_N8(int n8) {
        
        int vf = Alu.and(A(), n8);
        exec(tab[0xe6],new int[] {n8});
        assertEquals(Alu.unpackFlags(vf),F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void AND_A_B() {
        
        int vf = Alu.and(A(), B());
        exec(tab[0xa0],new int[] {});
        assertEquals(Alu.unpackFlags(vf),F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void AND_A_HLR() {
        
        int vf = Alu.and(A(), bus.read(HL()));
        exec(tab[0xa6],new int[] {});
        assertEquals(Alu.unpackFlags(vf),F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void OR_A_HLR() {
        
        int vf = Alu.or(A(), bus.read(HL()));
        exec(tab[0xb6],new int[] {});
        assertEquals(Alu.unpackFlags(vf),F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void XOR_A_HLR() {
        
        int vf = Alu.xor(A(), HLR());
        exec(tab[0xae],new int[] {});
        assertEquals(Alu.unpackFlags(vf),F());
        assertEquals(A(),Alu.unpackValue(vf));
    }
    
    private void CPL() {
        
        int v = Bits.complement8(A());
        int f = 0b0110_0000 | F();
        exec(tab[0x2f],new int[] {});
       
        assertEquals(f,F());
        assertEquals(A(),v);
    }
    
    private void SLA_B() {
        
        int vf = Alu.shiftLeft(B());
        exec(tab2[0x20],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),B());
        assertEquals(Alu.unpackFlags(vf),F());
    }
    
    private void SLA_HLR() {
        
        int vf = Alu.shiftLeft(bus.read(HL()));
        exec(tab2[0x26],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),bus.read(HL()));
        assertEquals(Alu.unpackFlags(vf),F());
    }
    
    private void SRA_HLR() {
        
        int vf = Alu.shiftRightA(bus.read(HL()));
        exec(tab2[0x2e],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),bus.read(HL()));
        assertEquals(Alu.unpackFlags(vf),F());
    }
    
    private void SRL_HLR() {
        
        int vf = Alu.shiftRightL(bus.read(HL()));
        exec(tab2[0x3e],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),bus.read(HL()));
        assertEquals(Alu.unpackFlags(vf),F());
    }
    
    private void RLCA() {
        
        int vf = Alu.rotate(Alu.RotDir.LEFT,A());
        int f = Alu.unpackFlags(vf);
        f = f&0b0001_0000;
        
        exec(tab[0x07],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),A());
        assertEquals(f,F());
    }
    
    private void RRCA() {
        
        int vf = Alu.rotate(Alu.RotDir.RIGHT,A());
        int f = Alu.unpackFlags(vf);
        f = f&0b0001_0000;
        
        exec(tab[0x0f],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),A());
        assertEquals(f,F());
    }
    
    private void RLA() {
        
        int vf = Alu.rotate(Alu.RotDir.LEFT,A(),Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        f = f&0b0001_0000;
        
        exec(tab[0x17],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),A());
        assertEquals(f,F());
    }
    
    private void RRA() {
        
        int vf = Alu.rotate(Alu.RotDir.RIGHT,A(),Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        f = f&0b0001_0000;
        
        exec(tab[0x1f],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),A());
        assertEquals(f,F());
    }
    
    private void RLC_A() {
        
        int vf = Alu.rotate(Alu.RotDir.LEFT,A());
        int f = Alu.unpackFlags(vf);
        f = f&0b1001_0000;
        
        exec(tab2[0x07],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),A());
        assertEquals(f,F());
    }
    
    private void RLC_HLR() {
        
        int vf = Alu.rotate(Alu.RotDir.LEFT,bus.read(HL()));
        int f = Alu.unpackFlags(vf);
        f = f&0b1001_0000;
        
        exec(tab2[0x06],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),bus.read(HL()));
        assertEquals(f,F());
    }
    
    private void RR_B() {
        
        int vf = Alu.rotate(Alu.RotDir.RIGHT,B(),Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        f = f&0b1001_0000;
        
        exec(tab2[0x18],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),B());
        assertEquals(f,F());
    }
    
    private void RR_HLR() {
        
        int vf = Alu.rotate(Alu.RotDir.RIGHT,bus.read(HL()),Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        f = f&0b1001_0000;
        
        exec(tab2[0x1e],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),bus.read(HL()));
        assertEquals(f,F());
    }
    
    private void SWAP_A() {
        
        int vf = Alu.swap(A());
        int f = Alu.unpackFlags(vf);
        f&=0b1000_0000;
        
        exec(tab2[0x37],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),A());
        assertEquals(f,F());
    }
    
    private void SWAP_HLR() {
        
        int vf = Alu.swap(HLR());
        int f = Alu.unpackFlags(vf);
        f&=0b1000_0000;
        
        exec(tab2[0x36],new int[] {});
        
        assertEquals(Alu.unpackValue(vf),HLR());
        assertEquals(f,F());
    }
    
    private void BIT_N3_R8(int n3,Reg r8) {
        
        int vf = Alu.testBit(reg(r8), n3);
        int f = Alu.unpackFlags(vf);
        f = (f&0b1000_0000) | 0b0010_0000 | (0b0001_0000 & F());

        exec(tab2[0b0100_0000 | (n3 << 3) | r8.index()],new int[] {});
        assertEquals(f,F());
    }
    
    private void BIT_N3_HLR(int n3) {
        
        int vf = Alu.testBit(HLR(), n3);
        int f = Alu.unpackFlags(vf);
        f = (f&0b1110_0000) | (0b0001_0000 & F());

        exec(tab2[0b0100_0000 | (n3 << 3) | 0b110],new int[] {});
        assertEquals(f,F());
    }
    
    private void SET_N3_R8(int n3, Reg r8, int b) {
        
        int v = Bits.set(reg(r8), n3, b == 1);
        int f = F();

        exec(tab2[0b1000_0000 | (b << 6) | (n3 << 3) | r8.index()],new int[] {});
        assertEquals(v,reg(r8));
        assertEquals(f,F());
    }
    
    private void SET_N3_HLR(int n3, int b) {
        
        int v = Bits.set(HLR(), n3, b == 1);
        int f = F();

        exec(tab2[0b1000_0000 | (b << 6) | (n3 << 3) | 0b110],new int[] {});
        assertEquals(v,HLR());
        assertEquals(f,F());
    }
    
    private void DAA() {
        
        int vf = Alu.bcdAdjust(A(), Bits.test(F(), Alu.Flag.N), Bits.test(F(), Alu.Flag.H), Bits.test(F(), Alu.Flag.C));
        int f = Alu.unpackFlags(vf);
        f = (f&0b1001_0000) | (F()&0b0100_0000);
        
        exec(tab[0x27],new int[] {});
        assertEquals(Alu.unpackValue(vf),A());
        assertEquals(f,F());
    }
    
    private void SCF() {
        
        int f = (F()&0b1000_0000) | 0b0001_0000;
        
        exec(tab[0x37],new int[] {});
        assertEquals(f,F());
    }
    
    private void CCF() {
        
        int c = Bits.test(F(), Alu.Flag.C) ? 0 : 1;
        int f = (F()&0b1000_0000) | (c << 4);
        
        exec(tab[0x3f],new int[] {});
        assertEquals(f,F());
    }
    
    private void fillRegistersWithRandomValues() {
        
        Random rng = newRandom();
        
        LD_A_N8(rng.nextInt(256));
        LD_B_N8(rng.nextInt(256));
        LD_C_N8(rng.nextInt(256));
        LD_D_N8(rng.nextInt(256));
        LD_E_N8(rng.nextInt(256));
        this.LD_BC_N16(rng.nextInt(256*256));
        this.LD_HL_SP_N8(rng.nextInt(256));
        LD_H_N8(200);
        LD_L_N8(rng.nextInt(256));
        
        this.ADD_A_B();
        this.ADD_HL_BC();
    }
    
    @Test 
    void testSomeInstructions(){
        
        init();
        LD_A_N8(234);
        LD_B_N8(51);
        LD_BC_N16(64000);
        
        ADC_A_N8(250);
        LD_HL_SP_N8(-5 + 256);
    }
    
    @Test 
    void additionsWithoutCarry(){
        
        Random rng = new Random();
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            this.ADD_A_B();
            this.ADD_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_HL_BC();
            this.ADD_HL_SP();
            this.ADD_SP_N(rng.nextInt(256));
            this.LD_HL_SP_N8(rng.nextInt(256));
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_HL_BC();
            this.INC_DE();
            this.ADD_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_HL_BC();
            this.ADD_A_B();
            this.ADD_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.INC_SP();
            this.INC_A();
            this.INC_HLR();
        }
    }
    
    @Test 
    void additionsWithCarry(){
        
        Random rng = new Random();
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_HL_BC();
            this.ADD_HL_SP();
            this.ADD_SP_N(rng.nextInt(256));
            this.LD_HL_SP_N8(rng.nextInt(256));
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_HL_BC();
            this.INC_DE();
            this.ADD_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_HL_BC();
            this.ADD_A_B();
            this.ADD_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            
            this.ADC_A_B();
            this.ADC_A_HLR();
            this.ADC_A_N8(rng.nextInt(256));
        }
    }
    
    @Test 
    void substractions() {
        
        Random rng = new Random();
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            
            this.SUB_A_N8(rng.nextInt(256));
            this.SBC_A_B();
            this.SBC_A_HLR();
            this.SBC_A_N8(rng.nextInt(256));
            this.DEC_A();
            this.DEC_HLR();
            this.DEC_SP();
            this.CP_A_B();
            this.CP_A_HLR();
            this.CP_A_N8(rng.nextInt(256));
        }
    }
    
    @Test 
    void bitwiseOperations() {
        
        Random rng = new Random();
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            
            this.AND_A_B();
            this.AND_A_HLR();
            this.AND_A_N8(rng.nextInt(256));
            this.OR_A_HLR();
            this.XOR_A_HLR();
            this.CPL();
        }
    }
    
    @Test 
    void shifts() {
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            
            this.SLA_B();
            this.SLA_HLR();
            this.SRL_HLR();
            this.SRA_HLR();
            this.SLA_B();
            this.SLA_HLR();
            this.SRL_HLR();
        }
    }
    
    @Test 
    void rotations() {
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            
            this.RLA();
            this.RLCA();
            this.RRA();
            this.RRCA();
            this.RLC_A();
            this.RLC_HLR();
            this.RR_B();
            this.RR_HLR();
            this.SWAP_A();
            this.SWAP_HLR();
        }
    }
    
    @Test
    void operationsOnBits() {
        
        Random rng = new Random();
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            
            this.BIT_N3_R8(rng.nextInt(8), registers[rng.nextInt(7)]);
            this.BIT_N3_HLR(rng.nextInt(8));
            this.SET_N3_R8(rng.nextInt(8), registers[rng.nextInt(7)], rng.nextInt(2));
            this.SET_N3_HLR(rng.nextInt(8), rng.nextInt(2));
        }
    }
    
    @Test
    void divers() {
                
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            fillRegistersWithRandomValues();
            
            this.DAA();
            this.CCF();
            this.SCF();
        }
    }
    
    @Test
    void meliMeloTuttiFrutti() {
        
        Random rng = new Random();
        
        for(int i=0;i<1e4;i++) {
                        
            init();
            
            this.SBC_A_B();
            this.RLC_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.DEC_SP();
            this.AND_A_N8(rng.nextInt(256));
            this.ADD_A_N8(rng.nextInt(256));
            this.SWAP_A();
            this.ADC_A_B();
            this.ADD_A_HLR();
            this.RR_HLR();
            this.CP_A_B();
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_SP_N(rng.nextInt(256));
            this.BIT_N3_HLR(rng.nextInt(8));
            this.ADD_A_N8(rng.nextInt(256));
            this.RRA();
            this.SWAP_A();
            this.SRL_HLR();
            this.ADD_SP_N(rng.nextInt(256));
            this.RR_HLR();
            this.ADD_A_B();
            this.RRCA();
            this.RLA();
            this.ADD_A_HLR();
            this.SLA_HLR();
            this.CCF();
            this.SCF();
            this.RLC_A();
            this.INC_SP();
            this.INC_HLR();
            this.RLC_A();
            this.SLA_B();
            this.INC_DE();
            this.SET_N3_R8(rng.nextInt(8), registers[rng.nextInt(7)], rng.nextInt(2));
            this.RR_B();
            this.ADD_A_HLR();
            this.ADC_A_HLR();
            this.BIT_N3_R8(rng.nextInt(8), registers[rng.nextInt(7)]);
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_HL_BC();
            this.OR_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.RRCA();
            this.ADD_A_B();
            this.DAA();
            this.RLCA();
            this.AND_A_B();
            this.RLC_HLR();
            this.SWAP_HLR();
            this.SBC_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_A_B();
            this.ADD_A_HLR();
            this.ADD_A_N8(rng.nextInt(256));
            this.ADD_A_HLR();
            this.RR_HLR();
            this.CP_A_HLR();
            this.SBC_A_N8(rng.nextInt(256));
            this.RLCA();
            this.RRA();
            this.INC_DE();
            this.XOR_A_HLR();
            this.SUB_A_N8(rng.nextInt(256));
            this.SRL_HLR();
            this.SLA_HLR();
            this.ADC_A_N8(rng.nextInt(256));
            this.INC_A();
            this.CPL();
            this.DEC_HLR();
            this.SLA_B();
            this.CP_A_N8(rng.nextInt(256));
            this.RLA();
            this.RR_B();
            this.DEC_A();
        }
    }
}
