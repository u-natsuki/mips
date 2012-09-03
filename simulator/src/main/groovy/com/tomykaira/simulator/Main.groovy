package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 10:44 PM
 * To change this template use File | Settings | File Templates.
 */
class Main {
    public static final String ENCODING = "UTF-8"

    public static void main(String[] args) {
        if (args.size() < 3) {
            println "Usage: instruction_file_name SLD_file_name output_ppm_file_name"
            System.exit(1)
        }

        def inst = new InstructionFile(new File(args[0]).getText(ENCODING))
        def server = new CServer(new File(args[1]).newInputStream())
        server.readSldFile()
        def mips = new Mips(inst, new Memory(), server)
        def prevPc = -1
        while (mips.pc != prevPc) {
            prevPc = mips.pc
            println mips.reg.get(31)
            println prevPc
            mips.tick()
        }
        new File(args[2]).write(server.ppm.toString(), ENCODING)
    }
}