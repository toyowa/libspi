# libspi

This is the spi library for raspberry Pi 3. Compile as follows.

gcc -std=c11 -fPIC -shared -o libspi.so spi.c

If you use this library from JAVA, 

export LD_LIBRARY_PATH=/to/your/library/libspi.so

$ sudo ldconfig

In Java Program, write

    interface SpiLib extends Library {
        SpiLib INSTANCE = (SpiLib) Native.loadLibrary("spi", SpiLib.class);
        int setupSpi(int ch, int ss, int spd);
        void transfer( byte[] tx, int size);
        void closeSpi();
    }

    public Accelerometer(){
        spi = SpiLib.INSTANCE;
        ........
        ........
    }

