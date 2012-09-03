package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */
class InstructionFile {
    private def instructions = []

    InstructionFile(String str) {
        if (str)
            instructions = str.split("\n")
    }

    def getLength() {
        instructions.size();
    }
}
