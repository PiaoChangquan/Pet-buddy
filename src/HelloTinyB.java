import tinyb.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class HelloTinyB {
    private static final float SCALE_LSB = 0.03125f;
    static boolean running = true;

    static void printDevice(BluetoothDevice device) {
        System.out.print("Address = " + device.getAddress());
        System.out.print(" Name = " + device.getName());
        System.out.print(" Connected = " + device.getConnected());
        System.out.println();
    }

    static float convertCelsius(int raw) {
        return raw / 128f;
    }  
    static float Gyroconvert(int raw) {
        return raw / (65536f / 500f);
    }
    static float Accconvert(int raw) {
        return raw / (32768f / 2f);
    } 
    static float Magconvert(int raw) {
        return raw * 1f;
    }
  
	
    /*
     * After discovery is started, new devices will be detected. We can get a list of all devices through the manager's
     * getDevices method. We can the look through the list of devices to find the device with the MAC which we provided
     * as a parameter. We continue looking until we find it, or we try 15 times (1 minutes).
     */
    static BluetoothDevice getDevice(String address) throws InterruptedException {
        BluetoothManager manager = BluetoothManager.getBluetoothManager();
        BluetoothDevice sensor = null;
        for (int i = 0; (i < 15) && running; ++i) {
            List<BluetoothDevice> list = manager.getDevices();
            if (list == null)
                return null;

            for (BluetoothDevice device : list) {
                printDevice(device);
                /*
                 * Here we check if the address matches.
                 */
                if (device.getAddress().equals(address))
                    sensor = device;
            }

            if (sensor != null) {
                return sensor;
            }
            Thread.sleep(4000);
        }
        return null;
    }

    /*
     * Our device should expose a temperature service, which has a UUID we can find out from the data sheet. The service
     * description of the SensorTag can be found here:
     * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf. The service we are looking for has the
     * short UUID AA00 which we insert into the TI Base UUID: f000XXXX-0451-4000-b000-000000000000
     */
    static BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
        System.out.println("Services exposed by device:");
        BluetoothGattService tempService = null;
        List<BluetoothGattService> bluetoothServices = null;
        do {
            bluetoothServices = device.getServices();
            if (bluetoothServices == null)
                return null;

            for (BluetoothGattService service : bluetoothServices) {
                System.out.println("UUID: " + service.getUuid());
                if (service.getUuid().equals(UUID))
                    tempService = service;
            }
            Thread.sleep(4000);
        } while (bluetoothServices.isEmpty() && running);
        return tempService;
    }

    static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        if (characteristics == null)
            return null;

        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (characteristic.getUuid().equals(UUID))
                return characteristic;
        }
        return null;
    }

    /*
     * This program connects to a TI SensorTag 2.0 and reads the temperature characteristic exposed by the device over
     * Bluetooth Low Energy. The parameter provided to the program should be the MAC address of the device.
     *
     * A wiki describing the sensor is found here: http://processors.wiki.ti.com/index.php/CC2650_SensorTag_User's_Guide
     *
     * The API used in this example is based on TinyB v0.3, which only supports polling, but v0.4 will introduce a
     * simplied API for discovering devices and services.
     */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {

        if (args.length < 1) {
            System.err.println("Run with <device_address> argument");
            System.exit(-1);
        }
        String path="/home/root/tinyb-master/tinyb-master/examples/java/SensorTagData.txt";
        File f = new File(path);
//        File f = new File(File.separator+"SensorTagData.txt");
        PrintWriter out = new PrintWriter(new FileOutputStream(f, true));
        /*
         * To start looking of the device, we first must initialize the TinyB library. The way of interacting with the
         * library is through the BluetoothManager. There can be only one BluetoothManager at one time, and the
         * reference to it is obtained through the getBluetoothManager method.
         */
        BluetoothManager manager = BluetoothManager.getBluetoothManager();

        /*
         * The manager will try to initialize a BluetoothAdapter if any adapter is present in the system. To initialize
         * discovery we can call startDiscovery, which will put the default adapter in discovery mode.
         */
        boolean discoveryStarted = manager.startDiscovery();

        System.out.println("The discovery started: " + (discoveryStarted ? "true" : "false"));
        BluetoothDevice sensor = getDevice(args[0]);

        /*
         * After we find the device we can stop looking for other devices.
         */
        manager.stopDiscovery();

        if (sensor == null) {
            System.err.println("No sensor found with the provided address.");
            System.exit(-1);
        }

        System.out.print("Found device: ");
        printDevice(sensor);

        if (sensor.connect())
            System.out.println("Sensor with the provided address connected");
        else {
            System.out.println("Could not connect device.");
            System.exit(-1);
        }

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                running = false;
                sensor.disconnect();
            }
        });


        BluetoothGattService tempService = getService(sensor, "f000aa00-0451-4000-b000-000000000000");
        BluetoothGattService HumidityService = getService(sensor, "f000aa20-0451-4000-b000-000000000000");
        BluetoothGattService MovementService = getService(sensor, "f000aa80-0451-4000-b000-000000000000");

//        if (tempService == null) {
//            System.err.println("This device does not have the temperature service we are looking for.");
//            sensor.disconnect();
//            System.exit(-1);
//        }
//        if (HumidityService == null) {
//            System.err.println("This device does not have the Humidity service we are looking for.");
//            sensor.disconnect();
//            System.exit(-1);
//        }
//        System.out.println("Found service " + tempService.getUuid());

        BluetoothGattCharacteristic tempValue = getCharacteristic(tempService, "f000aa01-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic tempConfig = getCharacteristic(tempService, "f000aa02-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic tempPeriod = getCharacteristic(tempService, "f000aa03-0451-4000-b000-000000000000");

        BluetoothGattCharacteristic HumidityValue = getCharacteristic(HumidityService, "f000aa21-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic HumidityConfig = getCharacteristic(HumidityService, "f000aa22-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic HumidityPeriod = getCharacteristic(HumidityService, "f000aa23-0451-4000-b000-000000000000");
        
     
        BluetoothGattCharacteristic MovementValue = getCharacteristic(MovementService, "f000aa81-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic MovementConfig = getCharacteristic(MovementService, "f000aa82-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic MovementPeriod = getCharacteristic(MovementService, "f000aa83-0451-4000-b000-000000000000");

        if (tempValue == null || tempConfig == null || tempPeriod == null ||
        		HumidityValue == null || HumidityConfig == null || HumidityPeriod == null||
        		MovementValue == null || MovementConfig == null || MovementPeriod == null)
             {
            System.err.println("Could not find the correct characteristics.");
            sensor.disconnect();
            System.exit(-1);
        }

        
        System.out.println("Found the temperature characteristics");

        /*
         * Turn on the Temperature Service by writing 1 in the configuration characteristic, as mentioned in the PDF
         * mentioned above. We could also modify the update interval, by writing in the period characteristic, but the
         * default 1s is good enough for our purposes.
         */
        byte[] config = { 0x01 };
        byte[] Mconfig = { 0x7f , 0x00};
        tempConfig.writeValue(config);
        HumidityConfig.writeValue(config);
        MovementConfig.writeValue(Mconfig);
        /*
         * Each second read the value characteristic and display it in a human readable format.
         */
        while (running) {
            byte[] tempRaw = tempValue.readValue();
            byte[] HumidityRaw = HumidityValue.readValue();
            byte[] MovementRaw = MovementValue.readValue();
            
            
            System.out.print("Temp raw = {");
            for (byte b : tempRaw) {
                System.out.print(String.format("%02x,", b));
            }
            System.out.println("}");
            System.out.print("Humidity raw = {");
            for (byte g : HumidityRaw) {
                System.out.print(String.format("%02x,", g));
            }
            System.out.println("}");

            System.out.print("Movement raw = {");
            for (byte g2 : MovementRaw) {
                System.out.print(String.format("%02x,", g2));
            }
            System.out.println("}");
          

            /*
             * The temperature service returns the data in an encoded format which can be found in the wiki. Convert the
             * raw temperature format to celsius and print it. Conversion for object temperature depends on ambient
             * according to wiki, but assume result is good enough for our purposes without conversion.
             */
            int objectTempRaw = (tempRaw[0] & 0xff) | (tempRaw[1] << 8);
            int ambientTempRaw = (tempRaw[2] & 0xff) | (tempRaw[3] << 8);

            int TempRaw = (HumidityRaw[0] & 0xff) | (HumidityRaw[1] << 8);
            int HumiRaw = (HumidityRaw[2] & 0xff) | (HumidityRaw[3] << 8);

            int GyroXRaw = (MovementRaw[0] & 0xff) | (MovementRaw[1] << 8);
            int GyroYRaw = (MovementRaw[2] & 0xff) | (MovementRaw[3] << 8);
            int GyroZRaw = (MovementRaw[4] & 0xff) | (MovementRaw[5] << 8);

            int AccXRaw = (MovementRaw[6] & 0xff) | (MovementRaw[7] << 8);
            int AccYRaw = (MovementRaw[8] & 0xff) | (MovementRaw[9] << 8);
            int AccZRaw = (MovementRaw[10] & 0xff) | (MovementRaw[11] << 8);
           
            int MagXRaw = (MovementRaw[12] & 0xff) | (MovementRaw[13] << 8);
            int MagYRaw = (MovementRaw[14] & 0xff) | (MovementRaw[15] << 8);
            int MagZRaw = (MovementRaw[16] & 0xff) | (MovementRaw[17] << 8);
            
            float objectTempCelsius = convertCelsius(objectTempRaw);
            float ambientTempCelsius = convertCelsius(ambientTempRaw);
            float TempRawCelsius = TempRaw/65536f*165-40;
            float HumiRawCelsius = HumiRaw/65536f*100;

            float GyroX = Gyroconvert(GyroXRaw);
            float GyroY = Gyroconvert(GyroYRaw);
            float GyroZ = Gyroconvert(GyroZRaw);

            float AccX = Accconvert(AccXRaw);
            float AccY = Accconvert(AccYRaw);
            float AccZ = Accconvert(AccZRaw);            
            
            float MagX = Magconvert(MagXRaw); 
            float MagY = Magconvert(MagYRaw); 
            float MagZ = Magconvert(MagZRaw);
            
            Date day=new Date();
			SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            System.out.println(
                    String.format(" Temp: Object = %fC, Ambient = %fC", objectTempCelsius, ambientTempCelsius));
            System.out.println(
            		String.format(" Temp: Object = %fC, Humi = %f", TempRawCelsius, HumiRawCelsius)+"%");
         
            System.out.println(String.format(" Gyroscop: x = %f'/s, y = %f'/s , x = %f'/s", GyroX, GyroY, GyroZ));
            System.out.println(String.format(" Accelerometer: x = %fG, y = %fG , x = %fG", AccX, AccY, AccZ));
            System.out.println(String.format(" Magnetometer: x = %fuT, y = %fuT , x = %fuT", MagX, MagY, MagZ));
            
            out.println("==================="+time.format(day)+"======================");
            out.println(
                    String.format(" Temp: Object = %fC, Ambient = %fC", objectTempCelsius, ambientTempCelsius));
            out.println(String.format(" Temp: Object = %fC, Humi = %f", TempRawCelsius, HumiRawCelsius)+"%");
            
            out.println(String.format(" Gyroscop: x = %f'/s, y = %f'/s , x = %f'/s", GyroX, GyroY, GyroZ));
            
            out.println(String.format(" Accelerometer: x = %fG, y = %fG , x = %fG", AccX, AccY, AccZ));
            
            out.println(String.format(" Magnetometer: x = %fuT, y = %fuT , x = %fuT", MagX, MagY, MagZ));
            
            
            Thread.sleep(1000);
        }
        sensor.disconnect();
        out.close();

    }
}
