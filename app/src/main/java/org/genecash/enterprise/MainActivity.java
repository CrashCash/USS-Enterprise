package org.genecash.enterprise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {
    //private static final String pkg = BuildConfig.APPLICATION_ID + ".";
    private static final String pkg = ".";
    private BluetoothLeScanner bleScanner;
    private BluetoothDevice device = null;
    private BluetoothGatt btGatt;
    private boolean scanning;
    private Handler handler;
    private boolean blinking;

    // these are hardcoded in the Playmobil app, so I feel safe hardcoding them here
    UUID uuid_chr = UUID.fromString("06d1e5e7-79ad-4a71-8faa-373789f7d93c");
    UUID uuid_svc = UUID.fromString("bc2f4cc6-aaef-4351-9034-d66268e328f0");

    // byte values are from "Sent Write Request" packets in wireshark dump of btsnoop_hci.log
    // I clicked each button one after the other in the Playmobil app
    List<Command> commands =
            Arrays.asList(new Command("aa070400ff", "", R.id.onoff),
                          new Command("aa0701ffff", "Warp Drive", R.id.bridge01),
                          new Command("aa070300ff", "Photon Torpedo", R.id.bridge02),
                          new Command("aa070200ff", "Alert", R.id.bridge03),
                          new Command("aa0801c8ff", "Run Astrogator", R.id.sound01),
                          new Command("aa0805c8ff", "Photon Torpedo Fire", R.id.sound02),
                          new Command("aa0803c8ff", "Bridge Ambient", R.id.sound03),
                          new Command("aa0802c8ff", "Boatswain Whistle", R.id.sound04),
                          new Command("aa0807c8ff", "Red Alert", R.id.sound05),
                          new Command("aa0804c8ff", "Dilithium Core Removed", R.id.sound06),
                          new Command("aa0806c8ff", "This Is Captain James Kirk", R.id.sound07),
                          new Command("aa080ac8ff", "Enter Warp Drive", R.id.sound08),
                          new Command("aa0808c8ff", "Live Long And Prosper", R.id.sound09),
                          new Command("aa080bc8ff", "Dematerialization", R.id.sound10),
                          new Command("aa0402c8ff", "Alert", R.id.light1),
                          new Command("aa0401c8ff", "Console Lights", R.id.light2),
                          new Command("aa0403c8ff", "Photon Torpedo Full", R.id.light3),
                          new Command("aa0406c8ff", "Dilithium Core", R.id.light4),
                          new Command("aa0407c8ff", "Warp Nacelles", R.id.light5),
                          new Command("aa0501c8ff", "Photon Torpedo Twice", R.id.light6),
                          new Command("aa0507c8ff", "Nacelles Warp Jump", R.id.light7));

    List<Button> blinkies;
    int blinky = 0;
    private TextView txtStatus;

    // step 1: this is called as a result of the initial startScan() in startScan()
    ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    device = result.getDevice();
                    log("BLE scan found: " + device.getAddress() + " " + device.getName() + " " + device.getAlias());
                    if (device.getName().startsWith("Pm_USSE_")) {
                        // we found it
                        log("found it - trying to connect");
                        status("Attempting to establish communications with starship");
                        bleScanner.stopScan(leScanCallback);
                        btGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
                    }
                    scanning = false;
                }
            };

    // step 2: this is called from the connectGatt() in the leScanCallback
    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("connected to the GATT Server");
                btGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                log("disconnected from the GATT Server");
                status("Lost communications with starship");
                startScan();
            }
        }

        // step 3: this is called from the discoverServices() in the gattCallback
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            log("status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                status("Communication established with starship");
                blinking = true;
                runOnUiThread(() -> blinkinlights());
                if (false) {
                    // spew a ton of debugging barf
                    for (BluetoothGattService service : btGatt.getServices()) {
                        log("service uuid: " + service.getUuid());
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            log("characteristic uuid: " + characteristic.getUuid() +
                                " write type: " + characteristic.getWriteType());
                        }
                    }
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            log("rssi: " + rssi + " status: " + status);
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic,
                                         @NonNull byte[] value, int status) {
            log("characteristic: " + characteristic + " value: " + value + " status: " + status);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic,
                                            @NonNull byte[] value) {
            log("characteristic: " + characteristic + " value: " + value);
        }

        // called after we do a command write
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            log("characteristic: " + characteristic + " status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //status("Command successful");
                status("");
            } else {
                status("Command failed");
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            log(" status: " + status);
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            log("");
        }
    };

    // get notified when bluetooth is turned on/off
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    case BluetoothAdapter.STATE_ON:
                        // the bluetooth adapter is on and ready for use
                        startScan();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        // the bluetooth adapter is off
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        // the bluetooth adapter is turning on
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        // the bluetooth adapter is about to turn off
                        if (btGatt != null) {
                            btGatt.close();
                        }
                        status("Bluetooth turned off. Communication lost.");
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanning = false;

        // hook up command buttons
        for (Command cmd : commands) {
            Button btn = findViewById(cmd.getId());
            btn.setText(cmd.getPrompt());
            btn.setOnClickListener(v -> command(cmd.getBytes()));
        }

        // for delayed actions
        handler = new Handler(Looper.getMainLooper());

        // hook up blinky lights
        blinkies = Arrays.asList(findViewById(R.id.blinky1), findViewById(R.id.blinky2),
                                 findViewById(R.id.blinky3), findViewById(R.id.blinky4),
                                 findViewById(R.id.blinky5), findViewById(R.id.blinky6),
                                 findViewById(R.id.blinky7), findViewById(R.id.blinky8));
        blinking = false;

        // volume slider
        SeekBar sldVolume = findViewById(R.id.volume);
        sldVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            // this is called every new value, which is too many times
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            // this is called when the user starts the slide
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            // this is called after the user stops sliding
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // scale to 0-255
                int value = (int) (seekBar.getProgress() * 12.8 - 0.01);
                // volume is aa03xx00ff where xx is 00 for silent, and FF for full blast
                // this doesn't work though, and I don't know why
                String cmd = "aa03" + String.format("%02X", value) + "00ff";
                command(cmd);
            }
        });
        sldVolume.setProgress(10);

        // status line at bottom
        txtStatus = findViewById(R.id.status);

        // request permissions if necessary
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    };
            // stupid asynchronous crap
            requestPermissions(permissions, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            // start bluetooth things now that we have permissions
            startScan();
        } else {
            txtStatus.setText("Bluetooth access is required to communicate with starship");
        }
    }

    // scan for BLE device
    void startScan() {
        log("");
        // uncomment to turn off bluetooth activity for faster UI display during development
        //scanning = true;
        if (!scanning && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            scanning = true;
            BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter.isEnabled()) {
                // we might be called from a non-UI thread
                status("Attempting to locate starship communication device");
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                bleScanner.startScan(leScanCallback);
            } else {
                // ask user to turn bluetooth on
                // the BroadcastReceiver will start scanning when it turns on
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // stupid asynchronous crap
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    // handle user turning on Bluetooth (or not)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // we shouldn't start scanning here as that will be done by the BroadcastReceiver
        if (resultCode != Activity.RESULT_OK) {
            txtStatus.setText("Bluetooth must be active to communicate with starship");
        }
    }

    @Override
    protected void onResume() {
        log("");
        registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        startScan();
        super.onResume();
    }

    @Override
    protected void onPause() {
        log("");

        // stop blinkylights
        blinking = false;
        handler.removeCallbacksAndMessages(null);

        // shut down Bluetooth gracefully
        unregisterReceiver(btReceiver);
        if (btGatt != null) {
            btGatt.close();
        }
        super.onPause();
    }

    // onPause may not always be called before onDestroy
    @Override
    protected void onDestroy() {
        log("");

        // stop blinkylights
        blinking = false;
        handler.removeCallbacksAndMessages(null);

        // shut down Bluetooth gracefully
        try {
            // no way to tell if a BroadcastReceiver is registered or not
            unregisterReceiver(btReceiver);
        } catch (Exception ignored) {
        }
        if (btGatt != null) {
            btGatt.close();
        }
        super.onDestroy();
    }

    // send BLE command
    void command(String cmd) {
        log(cmd);
        // parse command into bytes
        byte[] data = new BigInteger(cmd, 16).toByteArray();
        if (data.length == 6 && data[0] == 0) {
            // drop leading zero
            data = Arrays.copyOfRange(data, 1, data.length);
        }

        // validate it
        if ((data.length != 5) || (data[0] != (byte) 0xAA) || (data[4] != (byte) 0xFF)) {
            StringBuilder sb = new StringBuilder();
            for (byte b : data) {
                sb.append(String.format("%02X ", b));
            }
            log("invalid command bytes: " + sb);
            txtStatus.setText("Invalid command string: " + sb);
            return;
        }

        // send it!
        if (btGatt == null) {
            startScan();
        } else {
            BluetoothGattService btsvc = btGatt.getService(uuid_svc);
            if (btsvc != null) {
                BluetoothGattCharacteristic btchr = btsvc.getCharacteristic(uuid_chr);
                int status = btGatt.writeCharacteristic(btchr, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                log("result: " + status);
            }
        }
    }

    // continuously flash blinking lights
    void blinkinlights() {
        if (blinking) {
            Button x = blinkies.get(blinky);
            x.setPressed(false);
            blinky += 1;
            if (blinky >= blinkies.size()) {
                blinky = 0;
            }
            x = blinkies.get(blinky);
            x.setPressed(true);
            handler.postDelayed(this::blinkinlights, 750);
        }
    }

    // safely set status message from another thread
    void status(String msg) {
        runOnUiThread(() -> txtStatus.setText(msg));
    }

    // drop a debug log
    static void log(String msg) {
        Log.d(pkg, "DEBUG: " + whereAmI() + " " + msg);
    }

    // determine where we were called from
    static String whereAmI() {
        String call_class, call_method;
        List<String> methods = Arrays.asList("getThreadStackTrace", "getStackTrace", "whereAmI", "log", "logExcept", "setupLogging");
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            call_method = ste.getMethodName();
            if (!methods.contains(call_method)) {
                call_class = ste.getClassName();
                //call_class = call_class.substring(call_class.lastIndexOf(".") + 1);
                return pkg + call_class + ":" + call_method + ":";
            }
        }
        return null;
    }
}
