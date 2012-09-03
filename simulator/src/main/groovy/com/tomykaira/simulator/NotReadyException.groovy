package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 10:06 PM
 * To change this template use File | Settings | File Templates.
 */
class NotReadyException extends RuntimeException {
    NotReadyException() {
        super("CServer cannot send data before it receives 0xAA")
    }
}
