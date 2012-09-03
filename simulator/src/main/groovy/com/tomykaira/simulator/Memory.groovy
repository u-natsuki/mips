package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
class Memory {

    private final int MAX_ADDRESS = 2*1024*1024
    List<Long> mem = new ArrayList<Long>(MAX_ADDRESS)

    long get(address) {
        assertAddressInRange(address)
        if (mem[address] == null) {
            throw new NotYetSetException(address)
        } else {
            mem[address]
        }
    }

    void set(address, long data) {
        assertAddressInRange(address)
        mem[address.toInteger()] = data
    }

    private void assertAddressInRange(address) {
        if (address >= MAX_ADDRESS) {
            throw new OutOfRangeException();
        }
    }
}
