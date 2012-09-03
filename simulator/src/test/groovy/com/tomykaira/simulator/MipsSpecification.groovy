package com.tomykaira.simulator

import spock.lang.Specification
import com.sun.org.apache.bcel.internal.generic.Instruction
import groovy.mock.interceptor.MockFor

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
        def mips = init(addi(1, 0, 5), sw(1, 0, 1024))
        mips.tick().tick()

        then:
        mips.pc == 2
        memory.get(1024) == 5
    }

    def "addi with negative number"() {
        when:
        def mips = init(addi(1, 0, 5), addi(1, 1, -3), sw(1, 0, 1024))
        mips.tick().tick().tick()

        then:
        memory.get(1024) == 2
    }

    def "beq"() {
        when:
        def mips = init(addi(1,0,5), addi(2,0,5), beq(1,2,8))
        mips.tick().tick().tick()

        then:
        mips.pc == 11
    }

    def "floating 0"() {
        when:
        def mips = init(sw(16,0,1000))
        mips.tick()

        then:
        memory.get(1000) == 0
    }

    def "jump"() {
        when:
        def mips = init(addi(1,0,5), jump(0))
        mips.tick().tick()

        then:
        mips.pc == 0
    }

    def "funcall"() {
        when:
        def mips = init(addi(1,0,5), call(3), sw(1, 0, 1024), add(1,1,1), ret())

        times.times {mips.tick()}

        then:
        mips.pc == pc

        where:
        times | pc
        0     | 0
        1     | 1
        2     | 3
        3     | 4
        4     | 2
        5     | 3
    }

    def "send a byte"() {
        when:
        def received = null
        def mips = initWithPort([send: {arg -> received = arg}], addi(1,0,value), send(1))
        mips.tick().tick()

        then:
        received == sent

        where:
        value  | sent
        5      | 5
        0x41a  | 0x1a
    }

    def "receives an input"() {
        when:
        def mips = initWithPort([receive: {arg -> 72}], receive(5), sw(5, 0, 9))
        mips.tick().tick()

        then:
        memory.get(9) == 72
    }

    Mips init(String ...s) {
        new Mips(new InstructionFile(s.join("\n")), memory, null)
    }

    Mips initWithPort(port, String ...s) {
        new Mips(new InstructionFile(s.join("\n")), memory, port as Expando)
    }

    String receive(int reg) {
        "001100" + bit(0, 5) + bit(reg, 5) + bit(0, 16)
    }

    String send(int reg) {
        "000100" + bit(0, 5) + bit(reg, 5) + bit(0, 16)
    }

    String call(int pc) {
        "110111" + bit(pc, 26)
    }

    String jump(int pc) {
        "111111" + bit(pc, 26)
    }

    String ret() {
        "111000" + bit(0, 26)
    }

    String add(int rd, int rs, int rt) {
        "000010" + bit(rd, 5) + bit(rs, 5) + bit(rt, 5) + bit(0, 11)
    }

    String addi(int to, int from, int imm) {
        "001010" + bit(from, 5) + bit(to, 5) + bit(imm, 16)
    }

    String sw(int from, int address, int diff) {
        "101011" + bit(address, 5) + bit(from, 5) + bit(diff, 16)
    }

    String beq(int rs, int rt, int diff) {
        "111110" + bit(rs, 5) + bit(rt, 5) + bit(diff, 16)
    }

    String bit(Number number, int columns) {
        def positive = number > 0 ? number : 2 ** columns + number
        ("0"*columns + Integer.toString(positive, 2))[-columns..-1]
    }
}
