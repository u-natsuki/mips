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
        def memory = new Memory()
        server.readSldFile()
        def mips = new Mips(inst, memory, server)

        registerShutdownHandler(mips, memory)

        def prevPc = -1
        while (mips.pc != prevPc) {
            prevPc = mips.pc
            println prevPc
            try {
                mips.tick()
            } catch (Exception e) {
                System.err.println("${mips.pc}")
                e.printStackTrace()
                throw e
            }
        }
        new File(args[2]).write(server.ppm.toString(), ENCODING)
    }

    static void registerShutdownHandler(Mips mips, Memory memory) {

        System.err.println("Adding error handler")
        Runtime.runtime.addShutdownHook {
            System.err.println("Dumping")

            def regWriter = new FileWriter("reg_dump")
            def memoryWriter = new FileWriter("memory_dump")
            mips.reg.dump(regWriter)
            memory.dump(memoryWriter)
            regWriter.close()
            memoryWriter.close()
        }

    }
}
