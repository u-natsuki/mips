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
        def inst = new InstructionFile();

        then:
        inst.length == 0
    }
}
