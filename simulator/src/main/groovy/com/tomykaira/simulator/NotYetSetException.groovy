package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 3:20 PM
 * To change this template use File | Settings | File Templates.
 */
class NotYetSetException extends RuntimeException {
    NotYetSetException(Number address) {
        super("Address Violation: #${address} is not initialized")
    }

}
