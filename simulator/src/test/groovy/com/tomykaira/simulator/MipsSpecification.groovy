package com.tomykaira.simulator

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 4:11 PM
 * To change this template use File | Settings | File Templates.
 */
class MipsSpecification extends Specification {

    def memory = new Memory()

    def "addi sw"() {
        when:
        def inst = new InstructionFile([addi(1, 0, 5), sw(1, 0, 1024)].join("\n"))
        def mips = new Mips(inst, memory)
        mips.tick().tick()

        then:
        mips.pc == 2
        memory.get(1024) == 5
    }

    String addi(int to, int from, int imm) {
        "001010" + bit(from, 5) + bit(to, 5) + bit(imm, 16)
    }

    String sw(int from, int address, int diff) {
        "101011" + bit(address, 5) + bit(from, 5) + bit(diff, 16)
    }

    String bit(Number number, int columns) {
        ("0"*columns + Integer.toString(number, 2))[-columns..-1]
    }


}
