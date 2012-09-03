package com.tomykaira.simulator

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
    int pc = 0
    private final Register reg = new Register(32)

    Mips(InstructionFile instructionFile, Memory memory) {
        this.instructionFile = instructionFile
        this.memory = memory
    }

    Mips tick() {
        def inst = instructionFile.get(pc)
        def rs = (inst >> 21) & 0b11111
        def rt = (inst >> 16) & 0b11111
        def imm = inst & 0xffff
        switch (inst >> 26) {
            case 10:
                reg.set(rt, reg.get(rs) + imm)
            case 43:
                memory.set(reg.get(rs) + imm, reg.get(rt))
        }
        pc += 1
        return this
    }
}
