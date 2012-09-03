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
    private final InstructionFile instructionFile
    private final Memory memory
    private int pc = 0
    private final Register reg = new Register(32)

    Mips(InstructionFile instructionFile, Memory memory) {
        this.instructionFile = instructionFile
        this.memory = memory
        reg.set(31, 0)
    }

    /*
        when 'AND'  then '000000'
    when 'NOP'  then '000000'
    when 'OR'   then '000001'
    when 'ADD'  then '000010'
    when 'XOR'  then '000101'
    when 'SUB'  then '000110'
    when 'SLT'  then '000111'

    when 'ANDI' then '001000'
    when 'ORI'  then '001001'
    when 'ADDI' then '001010'
    when 'SLL'  then '001011'
    when 'SUBI' then '001110'
    when 'SLTI' then '001111'

     */

    Mips tick() {
        def inst = instructionFile.get(pc)
        def rs = (inst >> 21) & 0b11111
        def rt = (inst >> 16) & 0b11111
        def rd = (inst >> 11) & 0b11111
        def imm = inst & 0xffff
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
                reg.set(rd, ib(f(reg.get(rs)) + f(reg.get(rt))))
                break
            case 17:
                reg.set(rd, ib(f(reg.get(rs)) - f(reg.get(rt))))
                break
            case 18:
                reg.set(rd, ib(f(reg.get(rs)) * f(reg.get(rt))))
                break
            case 19:
                reg.set(rt, f(reg.get(rs)).toInteger())
                break
            case 20:
                reg.set(rt, ib(reg.get(rs).toFloat()))
                break
            case 4:
                throw new NotImplementedException("SNDB")
                break
            case 12:
                throw new NotImplementedException("RBYT")
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
                memory.set(reg.get(31), pc + 1)
                pc = jmp - 1 // adjustment for always +1
                break
            case 56:
                pc = memory.get(reg.get(31)) - 1
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
                pc = jmp
                break
        }
        pc += 1
        return this
    }

    long ib(float v) {
        Float.floatToIntBits(v).toLong()
    }

    float f(long l) {
        Float.intBitsToFloat(l.toInteger())
    }

    int getPc() { pc }
}
