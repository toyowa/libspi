import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.sun.jna.Library;
import com.sun.jna.Native;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Toyoaki WASHIDA <Toyoaki WASHIDA at ibot.co.jp>
 */
public class Accelerometer {
    int speed = 1000000; //Hz
    int SPI_CHANNEL0 = 0;
    int SPI_CHANNEL1 = 1;
    int SPI_SS0= 0;
    int SPI_SS1= 1;
    Pin SS_PORT_ORG = RaspiPin.GPIO_01; //18;
    Pin SS_PORT_TOP = RaspiPin.GPIO_21; //5;
    Pin SS_LEFT_LOWLEG = RaspiPin.GPIO_22; //6;
    Pin SS_LEFT_FOOT = RaspiPin.GPIO_23; //13;
    Pin SS_RIGHT_LOWLEG = RaspiPin.GPIO_26; //12;
    Pin SS_RIGHT_FOOT = RaspiPin.GPIO_25; //26;
    //Pin [] pins = {SS_PORT_TOP};
    //String [] sensorNames = {"TOP"};
    int sfd0 = 0;
    int sfd1 = 1;
   // int sfd2 = 2;
    int [] sfds = {sfd1,sfd1,sfd1,sfd0,sfd0};
    Pin [] pins = {SS_PORT_TOP,SS_LEFT_LOWLEG,SS_LEFT_FOOT,SS_RIGHT_LOWLEG,SS_RIGHT_FOOT};
    String [] sensorNames = {"TOP","LEFT_LOWLEG","LEFT_FOOT","RIGHT_LOWLEG","RIGHT_FOOT"};
    //Pin [] pins = {SS_PORT_TOP,SS_LEFT_LOWLEG,SS_LEFT_FOOT,SS_RIGHT_LOWLEG};
    //String [] sensorNames = {"TOP","LEFT_LOWLEG","LEFT_FOOT","RIGHT_LOWLEG"};

    SpiLib spi;
    GpioController gpio;
    GpioPinDigitalOutput [] outs;
    
    interface SpiLib extends Library {
        SpiLib INSTANCE = (SpiLib) Native.loadLibrary("spi", SpiLib.class);
        int setupSpi(int ch, int ss, int spd);
        void transfer(int sfd,  byte[] tx, int size);
        void closeSpi(int sfd);
        void getConfig();
        void setDelay(int gdelay);
        void setSpeed(int gspeed);
    }

    public Accelerometer(){
        spi = SpiLib.INSTANCE;
        gpio = GpioFactory.getInstance();
        outs = new GpioPinDigitalOutput[5];
        // setup SPI for communication
        System.out.println("Initialize KXSD9s");
        //spi.setDelay(200);
        //spi.getConfig();
        sfd0 = spi.setupSpi(SPI_CHANNEL0, SPI_SS1, speed);
        System.out.println("SPI_CHANNEL ["+SPI_CHANNEL0+"] SPI_SS ["+SPI_SS1+"] speed ["+speed+"] sfd = "+sfd0);
        sfd1 = spi.setupSpi(SPI_CHANNEL1, SPI_SS0, speed);
        System.out.println("SPI_CHANNEL ["+SPI_CHANNEL1+"] SPI_SS ["+SPI_SS0+"] speed ["+speed+"] sfd = "+sfd1);
        //sfd2 = spi.setupSpi(SPI_CHANNEL1, SPI_SS1, speed);
        //System.out.println("SPI_CHANNEL ["+SPI_CHANNEL1+"] SPI_SS ["+SPI_SS1+"] speed ["+speed+"] sfd = "+sfd2);

        for(int i=0;i<pins.length;i++){
            initialize(i);
            System.out.println("SS_PORT ["+sensorNames[i]+"] Gpio ["+pins[i].toString()+"] is initialized.");
            readSensorsStasut(i);
        }
    }
    
   public  void closeSpi(){
       gpio.shutdown();
       for(int i=0;i<pins.length;i++){
            spi.closeSpi(sfds[i]);
       }
    }
    
    private void initialize(int sno){
        //SS信号初期化
        GpioPinDigitalOutput out = gpio.provisionDigitalOutputPin(pins[sno], sensorNames[sno], PinState.HIGH);
        outs[sno] = out;
        byte com[] = new byte[2];
        com[0] = 0x0c;  // address byte
        com[1] = (byte) 0xe3;  // register byte
        // configure（+/-2g 819 counts/g）
        dataWrite(sfds[sno], out, com,2);
        com[0] = 0x0d;
        com[1] = 0x40; // this is default
        dataWrite(sfds[sno],out, com,2);
    }
    
    void dataWrite(int sfd, GpioPinDigitalOutput out, byte [] buffer, int size){
        out.low();
        spi.transfer(sfd,buffer, size);
        out.high();
       
        try {
           Thread.sleep(0, 130);
           //Thread.sleep(1);
        } catch (InterruptedException ex) {
            Logger.getLogger(Accelerometer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Gpio.delayMicroseconds(130);
    }
    
    public double[] sensorValue(int ss){
        // デバイスからデータ取得
        double [] sense = new double[3];
        byte [][] spi_buff = new byte[3][3];    //buffa for sending and receiving
        
        int xregH, xregL, xout;
        int yregH, yregL, yout;
        int zregH, zregL, zout;
        double xac, yac, zac;

        for (int i = 0; i < 3; i++) {
            spi_buff[i][0] = (byte)(0x80 + 2 * i);
            spi_buff[i][1] = 0;
            spi_buff[i][2] = 0;
            dataWrite(sfds[ss],outs[ss],spi_buff[i],3);
        }
        xregH = (spi_buff[0][1] & 0xff);
        xregL = (spi_buff[0][2] & 0xff);
        //System.out.println("xregH = "+xregH+" xregL = "+xregL); //(short) (valueByte & 0xFF);
        xout = xregH << 4 | xregL >> 4;

        yregH = (spi_buff[1][1] & 0xff);
        yregL = (spi_buff[1][2] & 0xff);
        yout = yregH << 4 | yregL >> 4;

        zregH = (spi_buff[2][1] & 0xff);
        zregL = (spi_buff[2][2] & 0xff);
        zout = zregH << 4 | zregL >> 4;

        xac = (double) (xout - 2048) / (double) 819;
        yac = (double) (yout - 2048) / (double) 819;
        zac = (double) (zout - 2048) / (double) 819;
        //
        sense[0] = xac;
        sense[1] = yac;
        sense[2] = zac;
        return sense;
    }
    
    public void accelerTets(int iter){
        System.out.println("Start catching sendor data. Total = " + iter);
        System.out.print("データ ");
        for(int s=0;s<pins.length;s++){
            System.out.print(sensorNames[s]+" X_axis Y_axis Z_axis ");
        }
        System.out.println();
        for (int i = 0; i <= iter; i++) {
            System.out.print(i);
            for(int s=0;s<pins.length;s++){
                double[] sdata = sensorValue(s);
                System.out.print(" " + sdata[0] + " " + sdata[1] + " " + sdata[2]);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Accelerometer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println();
        }
        closeSpi();
    }

    private void readSensorsStasut(int ss){
        byte com[] = new byte[2];
        com[0] = (byte) 0x8c;
        com[1] = 0x00; 
        dataWrite(sfds[ss],outs[ss],com,2);
        System.out.println("Value of CTL_REGC = "+String.format("0x%02x",com[1] ));
        //
        com[0] = (byte) 0x8d;
        com[1] = 0x00; // 
        dataWrite(sfds[ss],outs[ss],com,2);
        System.out.println("Value of CTL_REGB = "+String.format("0x%02x",com[1] ));
    }
}
