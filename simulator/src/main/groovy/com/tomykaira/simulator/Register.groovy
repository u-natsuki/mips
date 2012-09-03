package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
class Register {
    List<Long> regfile
    int length

    Register(int length) {
        this.length = length
        regfile = new ArrayList<Long>(length)
    }

    long get(long address) {
        if (address == 0 || address == 16)
            return 0
        if (address >= length)
            throw new OutOfRangeException()
        if (regfile[address.toInteger()] == null)
            throw new NotYetSetException(address)
        regfile[address.toInteger()]
    }

    void set(long address, long data) {
        if (address >= length)
            throw new OutOfRangeException()
        regfile[address.toInteger()] = data
    }
}
