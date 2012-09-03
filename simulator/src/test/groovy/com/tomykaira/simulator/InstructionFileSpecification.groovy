package com.tomykaira.simulator

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 2:29 PM
 * To change this template use File | Settings | File Templates.
 */
class InstructionFileSpecification extends Specification {

    def "When read a blank string"() {
        when:
        def inst = new InstructionFile()

        then:
        inst.length == 0
    }

    def "When a line is supplied"() {
        when:
        def inst = new InstructionFile("0"*32)

        then:
        inst.length == 1
        inst.get(0) == 0
    }

    def "Multiline"() {
        when:
        def inst = new InstructionFile("0"*32+"\n"+"1"*32)

        then:
        inst.length == 2
        inst.get(1) == 0xffffffff
    }

    def "blank lines should be skipped"() {
        when:
        def inst = new InstructionFile("0"*32+"\n\n"+"1"*32+"\n")

        then:
        inst.length == 2
    }
}
