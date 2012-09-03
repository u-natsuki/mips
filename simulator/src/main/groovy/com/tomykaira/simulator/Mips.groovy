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
                def result = f(reg.get(rs)) + f(reg.get(rt))
                if (result.isNaN())
                    throw new SimulationException("Result of ${rs}(${reg.get(rs)}) + ${rt}(${reg.get(rt)}) is NaN")

                reg.set(rd, ib(result))
                break
            case 17:
                def result = f(reg.get(rs)) - f(reg.get(rt))
                if (result.isNaN())
                    throw new SimulationException("Result of ${rs}(${reg.get(rs)}) - ${rt}(${reg.get(rt)}) is NaN")

                reg.set(rd, ib(result))
                break
            case 18:
                def result = f(reg.get(rs)) * f(reg.get(rt))
                if (result.isNaN())
                    throw new SimulationException("Result of ${rs}(${reg.get(rs)}) * ${rt}(${reg.get(rt)}) is NaN")

                reg.set(rd, ib(result))
                break
            case 19:
                reg.set(rt, f(reg.get(rs)).toInteger())
                break
            case 20:
                reg.set(rt, ib(reg.get(rs).toFloat()))
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

    private Number signExtend(Number number, int fromSize) {
        def cut = number & ((1 << fromSize) - 1)
        if ((cut & (1 << (fromSize-1))) > 0)
            cut - (1 << fromSize)
        else
            cut
    }

    private long ib(Number v) {
        Float.floatToIntBits(v.toFloat()).toLong()
    }

    private float f(long l) {
        Float.intBitsToFloat(l.toInteger())
    }

    int getPc() { pc }
}
