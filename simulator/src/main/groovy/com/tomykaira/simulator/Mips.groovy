package com.tomykaira.simulator

import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 4:31 PM
 * To change this template use File | Settings | File Templates.
 */
class Mips {
    public static final int STACK_SIZE = 64

    private final InstructionFile instructionFile
    private final Memory memory
    private int pc = 0
    private final Register reg = new Register(32)
    private final List<Integer> stack = new ArrayList<Integer>(STACK_SIZE)
    private final ioPort

    Mips(InstructionFile instructionFile, Memory memory, port) {
        this.instructionFile = instructionFile
        this.memory = memory
        this.ioPort = port
    }

    Mips tick() {
        def inst = instructionFile.get(pc)
        def rs = (inst >> 21) & 0b11111
        def rt = (inst >> 16) & 0b11111
        def rd = (inst >> 11) & 0b11111
        def imm = signExtend(inst, 16)
        def jmp = inst & 0x3ffffff
        switch (inst >> 26) {
            case 0:
                reg.set(rd, reg.get(rs) & reg.get(rt))
                break
            case 1:
                reg.set(rd, reg.get(rs) | reg.get(rt))
                break
            case 2:
                reg.set(rd, reg.get(rs) + reg.get(rt))
                break
            case 5:
                reg.set(rd, reg.get(rs).xor(reg.get(rt)))
                break
            case 6:
                reg.set(rd, reg.get(rs) - reg.get(rt))
                break
            case 7:
                reg.set(rd, reg.get(rs) < reg.get(rt) ? 1 : 0)
                break
            case 10:
                reg.set(rt, reg.get(rs) + imm)
                break
            case 11:
                reg.set(rt, reg.get(rs) << imm)
                break
            case 16:
                def result = fadd(reg.get(rs).toInteger(), reg.get(rt).toInteger())
                if (Float.intBitsToFloat(result).isNaN())
                    throw new SimulationException("Result of ${rs}(${reg.get(rs)}) + ${rt}(${reg.get(rt)}) is NaN")

                reg.set(rd, result)
                break
            case 17:
                def result = fadd(reg.get(rs).toInteger(), reg.get(rt).xor(0x80000000).toInteger())
                if (Float.intBitsToFloat(result).isNaN())
                    throw new SimulationException("Result of ${rs}(${reg.get(rs)}) - ${rt}(${reg.get(rt)}) is NaN")

                reg.set(rd, result)
                break
            case 18:
                def result = f(reg.get(rs)) * f(reg.get(rt))
                if (result.isNaN())
                    throw new SimulationException("Result of ${rs}(${reg.get(rs)}) * ${rt}(${reg.get(rt)}) is NaN")

                reg.set(rd, ib(result))
                break
            case 19:
                reg.set(rt, f2i(reg.get(rs).toInteger(), false))
                break
            case 20:
                reg.set(rt, i2f(reg.get(rs).toInteger()))
                break
            case 4:
                ioPort.send((reg.get(rt) & 0xff).toInteger())
                break
            case 12:
                reg.set(rt, ioPort.receive())
                break
            case 35:
                reg.set(rt, memory.get(reg.get(rs) + imm))
                break
            case 36:
                reg.set(rt, memory.get(reg.get(rs) + reg.get(rd)))
                break
            case 43:
                memory.set(reg.get(rs) + imm, reg.get(rt))
                break
            case 44:
                memory.set(reg.get(rs) + reg.get(rd), reg.get(rt))
                break
            case 55:
                if (stack.size() >= 64)
                    throw new SimulationException("Stack overflow")
                stack.push(pc + 1)
                pc = jmp - 1 // adjustment for always +1
                break
            case 56:
                pc = stack.pop() - 1
                break
            case 57:
            case 60:
                if (reg.get(rs) >= reg.get(rt))
                    pc += imm
                break
            case 58:
            case 61:
                if (reg.get(rs) < reg.get(rt))
                    pc += imm
                break
            case 59:
            case 62:
                if (reg.get(rs) == reg.get(rt))
                    pc += imm
                break
            case 63:
                pc = jmp - 1 // adjustment for always +1
                break
            default:
                System.err.println("Instruction not implemented: ${pc}: ${inst}")
                throw new NotImplementedException()
        }
        pc += 1
        return this
    }

    int i2f(int x) {
        int s = 0

        if (x == 0) return 0

        if (x < 0){
            s = 0x80000000
            x = -x
        }

        int e = 150

        while (x >= 0x01000000){
            e++
            x >>= 1
        }
        while (x < 0x00800000){
            e--
            x <<= 1
        }

        return s | (e << 23) | (x & 0x007FFFFF)
    }

    int f2i(int x, boolean opCode) {
        boolean s = (x & 0x80000000) != 0;
        int e = ((x & 0x7F800000) >> 23) - 127;
        int m = (x & 0x007FFFFF) | 0x00800000;

        int tmp;
        int guard = 0, nonzero = 0;

        if ((x & 0x7FFFFFFF) == 0) return 0;    // ±0
        if (e >= 31){
            tmp = 0x7FFFFFFF;
            guard = nonzero = 0;
        } else if (e <= -2) {                   // ±0.5 以内
            tmp = guard = 0;
            nonzero = 1;
        } else {                                // それ以外
            if (e >= 23) {                      // 左にシフト
                tmp = m << (e - 23);
                guard = nonzero = 0;
            } else {                            // 右にシフト
                tmp = m;
                for (; e < 23; e++){
                    guard = tmp & 1;
                    nonzero |= guard;
                    tmp >>= 1;
                }
            }
        }

        if (opCode){    // floor（整数版）
            if (s){
                if (nonzero != 0){
                    return -tmp - 1;
                } else {
                    return -tmp;
                }
            } else {
                return tmp;
            }
        } else {    // f2i
            if (s){
                return -tmp - guard;
            } else {
                return tmp + guard;
            }
        }
    }

    int fadd(int x, int y) {

        int g, l;

        if ((x & 0x7FFFFFFF) >= (y & 0x7FFFFFFF)){
            g = x;
            l = y;
        } else {
            g = y;
            l = x;
        }

        int g_s = g & 0x80000000, l_s = l & 0x80000000;
        int g_e = (g & 0x7F800000) >> 23, l_e = (l & 0x7F800000) >> 23;
        int g_m = g & 0x007FFFFF, l_m = l & 0x007FFFFF;

        if (g_e != 0) g_m |= 0x00800000;    // この辺は IEEE に準拠していない
        if (l_e != 0) l_m |= 0x00800000;

        if (g_s == l_s){    // 加算
            l_m = (g_e - l_e <= 25) ? l_m >> (g_e - l_e) : 0;
            g_m += l_m;
            if (g_m >= 0x01000000){
                g_e++;
                g_m >>= 1;
            }
        } else {    // 減算
            if (g_e == l_e){
                g_m -= l_m;
                while (g_m < 0x00800000 && g_e > 0){
                    g_e--;
                    g_m <<= 1;
                }
            } else if (g_e - l_e == 1){
                g_m <<= 1;
                g_m -= l_m;

                if (g_m >= 0x01000000){
                    g_m >>= 1;
                } else {
                    g_e--;
                    if (g_e == 0) g_m >>= 1;
                    while (g_m < 0x00800000 && g_e > 0){
                        g_e--;
                        g_m <<= 1;
                    }
                }
            } else {
                g_m <<= 2;
                if (g_e - l_e == 0){
                    l_m <<= 2;
                } else if (g_e - l_e == 1){
                    l_m <<= 1;
                } else if (g_e - l_e <= 25){
                    l_m >>= (g_e - l_e - 2);
                } else {
                    l_m = 0;
                }

                g_m -= l_m;
                if (g_m >= 0x02000000){
                    g_m >>= 2;
                } else if (g_e > 0){
                    g_e--;
                    g_m >>= 1;
                }
            }
        }

        if (g_e == 0){
            return g_s;
        } else {
            return g_s | (g_e << 23) | (g_m & 0x007FFFFF);
        }
    }

    private int signExtend(Number number, int fromSize) {
        def cut = number & ((1 << fromSize) - 1)
        if ((cut & (1 << (fromSize-1))) > 0)
            (cut - (1 << fromSize)).toInteger()
        else
            cut.toInteger()
    }

    private int ib(Number v) {
        Float.floatToIntBits(v.toFloat())
    }

    private float f(int l) {
        Float.intBitsToFloat(l.toInteger())
    }

    int getPc() { pc }
}
