/*
 * libspi.so ver.2
 * License of this software is described in the head of the following text.
 * https://raw.githubusercontent.com/raspberrypi/linux/rpi-3.10.y/Documentation/spi/spidev_test.c
 * Compile is to be done as follows.
 * gcc -std=c11 -fPIC -shared -o libspi.so spi.c
 */

#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>
//#include <stdlib.h>
#include <fcntl.h>
#include <sys/ioctl.h>
//#include <linux/types.h>
#include <linux/spi/spidev.h>

char device[100] = "/dev/spidev0.0"; // default is no used
uint8_t mode = 0;
uint8_t bits = 8;
uint32_t speed = 1000000;
uint16_t delay = 0;
int fd[5];
int sdescri = 0; // device descriptor

void closeSpi(int sd) {
    close(fd[sd]);
}

void transfer(int sd, uint8_t tx[] , int size) {
    int ret;
    uint8_t rx[size];
    for(int i=0;i<size;i++){
        rx[i] = 0;
        //printf("tx[%d] = 0x%hhx\n",i,tx[i]);
    }
    struct spi_ioc_transfer tr = {
        .tx_buf = (unsigned long) tx,
        .rx_buf = (unsigned long) rx,
        .len = size,
        .delay_usecs = delay,
        .speed_hz = speed,
        .bits_per_word = bits,
    };

    ret = ioctl(fd[sd], SPI_IOC_MESSAGE(1), &tr);
    if (ret < 1){
        printf("can't send spi message\n");
        return;
    }
    for (ret = 0; ret < size; ret++) {
        tx[ret] = rx[ret];
    }
}

int setupSpi(int ch, int ss, int spd) {
    //printf("Start  sdescri = %d\n",sdescri);
    if(sdescri == 5){
        printf("Too may devices opened\n");
        return -1;
    }
    if(ch < 0 || ch > 1 || ss < 0 || ss>2){
        printf("channel No. or Slave dev No. illegal!!\n");
        return -1;
    }
    int ret = 0;
    speed = spd;
    sprintf(device, "/dev/spidev%d.%d", ch, ss);
    printf("Device = %s\n",device);
    
    fd[sdescri] = open(device, O_RDWR);
    if (fd[sdescri] < 0){
        printf("can't open device\n");
        return -1;
    }
    /*
     * spi mode
     */
    ret = ioctl(fd[sdescri], SPI_IOC_WR_MODE, &mode);
    if (ret == -1){
        printf("can't set spi mode");
        return -1;
    }

    ret = ioctl(fd[sdescri], SPI_IOC_RD_MODE, &mode);
    if (ret == -1){
        printf("can't get spi mode");
        return -1;
    }
    /*
     * bits per word
     */
    ret = ioctl(fd[sdescri], SPI_IOC_WR_BITS_PER_WORD, &bits);
    if (ret == -1){
        printf("can't set bits per word");
        return -1;
    }
    ret = ioctl(fd[sdescri], SPI_IOC_RD_BITS_PER_WORD, &bits);
    if (ret == -1){
        printf("can't get bits per word");
        return -1;
    }
    /*
     * max speed hz
     */
    ret = ioctl(fd[sdescri], SPI_IOC_WR_MAX_SPEED_HZ, &speed);
    if (ret == -1){
        printf("can't set max speed hz");
        return -1;
    }
    ret = ioctl(fd[sdescri], SPI_IOC_RD_MAX_SPEED_HZ, &speed);
    if (ret == -1){
        printf("can't get max speed hz");
        return -1;
    }
    //printf("Setup with Channel [%d] SlavePort [%d] Speed [%u] Mode [%hhu] bits/w [%hhu]\n",
    //        ch, ss, speed, mode, bits);
    sdescri++;
    //printf("End  sdescri = %d\n",sdescri);
    // return the device descriptor
    return sdescri-1;
}

void getConfig(){
    printf("spi mode = %hhu\n",mode);
    printf("delay= %hu\n",delay);
    printf("speed = %u\n",speed);
    printf("bits per word = %hhu\n",bits);
}

void setMode(int gmode){
    printf("Set spi mode = %d\n",gmode);
    mode = gmode;
}

void setDelay(int gdelay){
    printf("Set delay= %d\n",gdelay);
    delay = gdelay;
}

void setSpeed(int gspeed){
    printf("Set speed = %d\n",gspeed);
    speed = gspeed;
}
