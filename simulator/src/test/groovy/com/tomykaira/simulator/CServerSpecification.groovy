package com.tomykaira.simulator

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 8:33 PM
 * To change this template use File | Settings | File Templates.
 */
class CServerSpecification extends Specification {
    def sld = ClassLoader.getSystemResourceAsStream("contest.sld")
    def server = new CServer(sld)

    def "read environment"() {
        when:
        server.readSldEnv()
        server.readObject()

        then:
        server.data.size == 27
        server.data[0..8] == [-70, 35, -20, 20, 30, 1, 50, 50, 255].collect { Float.floatToIntBits(it.toFloat()) }
    }

    def "read network"() {
        when:
        def partial = new ByteArrayInputStream("5 6 7 -1 8 -1 9 10 -1 12 -1 13 -1 14 -1 15 -1 16 -1 -1".getBytes())
        def server = new CServer(partial)
        server.readNetwork()

        then:
        server.data.size == 20
    }

    def "raise an error is the input is too long"() {
        when:
        def fake = new ByteArrayInputStream(("0 "*5000 + "-1 -1").getBytes())
        def server = new CServer(fake)
        server.readNetwork()

        then:
        thrown(InvalidSLDException)
    }

    def "read all the file"() {
        when:
        server.readSldFile()

        then:
        server.data.size == 325
    }

    def "receive raises an exception before 0xaa"() {
        when:
        server.receive()

        then:
        thrown(NotReadyException)
    }

    def "send a data in sequence after 0xaa"() {
        when:
        server.readSldFile()
        server.send(0xaa)

        then:
        server.receive() == Float.floatToIntBits(-70)
        server.receive() == Float.floatToIntBits(35)
    }

    def "save received PPM data"() {
        when:
        server.send(0xaa)
        server.send(88) // = 'X'

        then:
        server.ppm.toString() == "X"
    }
}
