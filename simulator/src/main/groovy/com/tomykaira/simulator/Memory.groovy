package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
class Memory implements Serializable {

    private final int MAX_ADDRESS = 2*1024*1024
    int [] mem = new int [MAX_ADDRESS]
    boolean [] wroteFlag = new boolean [MAX_ADDRESS]

    int get(long address) {
        assertAddressInRange(address)
        def normalized = normalize(address)
        if (! wroteFlag[normalized]) {
            throw new NotYetSetException(address)
        } else {
            mem[normalized]
        }
    }

    void set(long address, int data) {
        assertAddressInRange(address)
        def normalized = normalize(address)
        mem[normalized] = data
        wroteFlag[normalized] = true
    }

    void dump(FileWriter st) {
        MAX_ADDRESS.times { st.write("${it}: ${mem[it]}\n") }
    }

    private void assertAddressInRange(address) {
        if (address >= MAX_ADDRESS) {
            throw new OutOfRangeException();
        }
    }

    private int normalize(address) {
        assertAddressInRange(address)
        if (address < 0) {
            (MAX_ADDRESS+address).toInteger()
        } else {
            address.toInteger()
        }
    }
}
