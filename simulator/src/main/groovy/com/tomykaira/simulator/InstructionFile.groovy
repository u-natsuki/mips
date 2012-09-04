package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */
class InstructionFile implements Serializable {
    private def instructions = []

    InstructionFile(String str) {
        if (str) {
            instructions = str.split("\n").findAll {it != ""}.collect {Long.parseLong(it, 2)}
        }
    }

    def getLength() {
        instructions.size();
    }

    long get(Integer integer) {
        instructions[integer]
    }
}
