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
    List<Integer> mem = new ArrayList<Integer>(MAX_ADDRESS)

    int get(long address) {
        assertAddressInRange(address)
        if (mem[address.toInteger()] == null) {
            throw new NotYetSetException(address)
        } else {
            mem[normalize(address)]
        }
    }

    void set(long address, int data) {
        assertAddressInRange(address)
        mem[normalize(address)] = data
    }

    private void assertAddressInRange(address) {
        if (address >= MAX_ADDRESS) {
            throw new OutOfRangeException();
        }
    }

    private int normalize(address) {
        assertAddressInRange(address)
        if (address < 0) {
            (MAX_ADDRESS-address).toInteger()
        } else {
            address.toInteger()
        }
    }
}
