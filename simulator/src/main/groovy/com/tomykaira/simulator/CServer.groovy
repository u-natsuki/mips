package com.tomykaira.simulator

/**
 * Created with IntelliJ IDEA.
 * User: tomykaira
 * Date: 9/3/12
 * Time: 9:30 PM
 * To change this template use File | Settings | File Templates.
 */
class CServer {
    private static final MAX_SLD_SIZE = 4096
    private final Scanner sldScan
    private boolean aaReceived = false
    List<Integer> data = new ArrayList<Integer>()
    StringBuffer ppm = new StringBuffer()

    CServer(InputStream inputStream) {
        sldScan = new Scanner(inputStream)
    }

    // Read the given SLD file into data
    void readSldFile() {
        readSldEnv()
        readObjects()
        readNetwork() // and network
        readNetwork() // or network
    }

    void readSldEnv() {
        // screen pos
        readFloat(); readFloat(); readFloat()
        // screen rotation
        readFloat(); readFloat()
        // n_lights : Actually, it should be an int value !
        readFloat()
        // light rotation
        readFloat(); readFloat()
        // beam
        readFloat()
    }

    void readObjects() {
        while (readInt() != -1) {
            readObject()
        }
    }

    void readNetwork() {
        while (readInt() != -1) {
            while (readInt() != -1) {}
        }
    }

    void readObject() {
        // form
        readInt()
        // refltype
        readInt()
        // isrot_p
        int is_rot = readInt()
        // abc
        readFloat(); readFloat(); readFloat()
        // xyz
        readFloat(); readFloat(); readFloat()
        // is_invert
        readFloat()
        // refl_param
        readFloat(); readFloat()
        // color
        readFloat(); readFloat(); readFloat()
        // rot
        if (is_rot) {
            readFloat(); readFloat(); readFloat()
        }
    }

    private int readInt() {
        return addIntData(sldScan.nextInt())
    }

    private float readFloat() {
        def ret = sldScan.nextFloat()
        addIntData(Float.floatToIntBits(ret))
        return ret
    }

    private int addIntData(int i) {
        if (data.size() >= MAX_SLD_SIZE)
            throw new InvalidSLDException("SLD file is too large.  Max size is ${MAX_SLD_SIZE}")

        data.push(i)
        return i
    }

    int receive() {
        if (! aaReceived)
            throw new NotReadyException()

        data.remove(0)
    }

    void send(int b) {
        if (b == 0xaa && ! aaReceived) {
            aaReceived = true
        } else {
            ppm.append(b as char)
        }
    }
}
