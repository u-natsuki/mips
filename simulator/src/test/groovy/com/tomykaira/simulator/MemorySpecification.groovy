package com.tomykaira.simulator

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
class MemorySpecification extends Specification {
    def memory = new Memory()

    def "raise an exception if the accessed word is not set"() {
        when:
        memory.get(1023)

        then:
        thrown(NotYetSetException)
    }

    def "return the set value when accessed"() {
        when:
        memory.set(1023, 503)

        then:
        memory.get(1023) == 503
    }

    def "set very big address"() {
        when:
        memory.set(20_0000_0000, 0)

        then:
        thrown(OutOfRangeException)
    }

    def "get very big address"() {
        when:
        memory.get(20_0000_0000)

        then:
        thrown(OutOfRangeException)
    }
}
