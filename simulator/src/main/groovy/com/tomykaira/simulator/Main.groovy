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

    private final InstructionFile instructionFile
    private final CServer server
    private final Memory memory
    private final Mips mips

    Main(InstructionFile instFile, CServer cs, Memory mem) {
        instructionFile = instFile
        server = cs
        memory = mem
        mips = new Mips(instructionFile, memory, server)

        registerShutdownHandler()
    }

    public static void main(String[] args) {
        if (args.size() < 3) {
            println "Usage: instruction_file_name SLD_file_name output_ppm_file_name"
            System.exit(1)
        }

        def server = new CServer(new File(args[1]).newInputStream())
        server.readSldFile()

        new Main(new InstructionFile(new File(args[0]).getText(ENCODING)), server, new Memory()).run()

        new File(args[2]).write(server.ppm.toString(), ENCODING)
    }

    void run() {
        def prevPc = -1
        while (mips.pc != prevPc) {
            prevPc = mips.pc
            println prevPc
            if (prevPc == 1630) {
                dumpRegister("1630")
            }
            try {
                mips.tick()
            } catch (Exception e) {
                System.err.println("${mips.pc}")
                e.printStackTrace()
                throw e
            }
        }
    }

    private void dumpMemory(String suffix) {
        def memoryWriter = new FileWriter("memory_dump${suffix}")
        memory.dump(memoryWriter)
        memoryWriter.close()
    }

    private void dumpRegister(String suffix) {
        def regWriter = new FileWriter("reg_dump${suffix}")
        mips.reg.dump(regWriter)
        regWriter.close()
    }

    private void dumpPPM(String suffix) {
        def ppmWriter = new FileWriter("ppm_dump${suffix}")
        server.dump(ppmWriter)
        ppmWriter.close()
    }

    private Closure dump(String suffix) {
        { ->
            System.err.println("Dumping")
            dumpMemory(suffix)
            dumpRegister(suffix)
            dumpPPM(suffix)
        }
    }

    private void registerShutdownHandler() {

        System.err.println("Adding error handler")
        Runtime.runtime.addShutdownHook(dump(new Date().format("HH-mm-ss")))

    }
}
