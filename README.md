# libspi.c

This is a spi library for raspberry Pi 3. Compile as follows.

gcc -std=c11 -fPIC -shared -o libspi.so spi.c

If you use this library from JAVA, 

export LD_LIBRARY_PATH=/to/your/library/libspi.so

$ sudo ldconfig

# Accelerometer.java

Accelerometer.java is just an exsample to use libspi.so with multiple Digital accelerometer KXSD9s.
