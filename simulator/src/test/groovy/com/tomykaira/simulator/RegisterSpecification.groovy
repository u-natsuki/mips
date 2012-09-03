package com.tomykaira.simulator

import spock.lang.Specification

/**
 * Specification for Register
 * User: tomykaira
 * Date: 9/3/12
 * Time: 3:45 PM
 */
class RegisterSpecification extends Specification {
    def reg = new Register(32)

    def "access to uninitialized register raises an exception"() {
        when:
        new Register(32).get(1)

        then:
        thrown(NotYetSetException)
    }

    def "access to 0 always returns 0"() {
        expect:
        reg.get(0) == 0
    }

    def "set out of range"() {
        when:
        reg.set(32, 0)

        then:
        thrown(OutOfRangeException)
    }

    def "get out of range"() {
        when:
        reg.get(32)

        then:
        thrown(OutOfRangeException)
    }
}
