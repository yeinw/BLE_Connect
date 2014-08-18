package com.example.test_03;

import java.util.ArrayList;
import java.util.List;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class HRSActivity extends Activity {
	
	private static final int REQUEST_ENABLE_BT = 2;

	Button connectBtn;
	
	
	public BluetoothAdapter mBluetoothAdapter;
	public boolean mScanning;
	public Handler mHandler;
	public Handler mHandler2;
	private static final long SCAN_PERIOD = 1000; // Stops scanning after 0.5 seconds.
	private static final long SCAN_PERIOD2 = 2000; // start connecting after 1 seconds.
   
	
	public final static String TAG = HRSActivity.class.getSimpleName();
	public BluetoothLeService mBluetoothLeService;
	public String mDeviceName;
	public String mDeviceAddress;
	public boolean mConnected = false;
	
	
	
	public BluetoothGattCharacteristic mNotifyCharacteristic;
	TextView mConnectionState;
	TextView mDataField;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	
	private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    
    private GraphViewSeries graphviewSeries;
    private GraphView graphView;
    private double graph2LastXValue = 5d;
    private double YValue = 0;
    
    private final Handler GraphHandler = new Handler();
    private Runnable mTimer2;
	
	
	
	public final ServiceConnection mServiceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
	
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
			// Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
          
           
        }
            
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService = null;
		}
		
		
	};
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			final String action = intent.getAction();
			
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                connectBtn.setText("DISCONNECT");
                updateConnectionState(R.string.connected);
                
			}
			
			else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                connectBtn.setText("CONNECT");
                //clearUI();
            } 
			
			else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
				
            } 
			
			else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                YValue = Double.valueOf(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                
            }
                
            
		}
		
	};
		

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrs);
        
        
    	BLEsupportCheck();
    	
    	mHandler = new Handler();
    	mHandler2 = new Handler();
    	mConnectionState = (TextView)findViewById(R.id.connection_state);
    	mDataField = (TextView) findViewById(R.id.Data_view);
    
        connectBtn = (Button)findViewById(R.id.connect_btn); 
        connectBtn.setOnClickListener(new OnClickListener() 
        {
            @Override
            public void onClick(View v) {
            	
            	if(mConnected){ 
            		Connect_cancle();	
            		drawCancle();
            		
            	}
            	else{ 
            		BLEScanning();
            		drawLine();
            	}
            	
              
            }   
        });   
        
        
      
        
        graphviewSeries = new GraphViewSeries(new GraphViewData[] {});
        
        
        graphView = new LineGraphView(
            this
            , "GraphViewDemo"
        );
        
        // add data
        graphView.addSeries(graphviewSeries);
        
        graphView.getGraphViewStyle().setNumHorizontalLabels(5);
        
        // set view port, start=2, size=40
        graphView.setViewPort(1,40); 
        graphView.setScrollable(true);
        // optional - activate scaling / zooming
        graphView.setScalable(true);
        
                 
        LinearLayout layout = (LinearLayout) findViewById(R.id.linear);
        layout.addView(graphView);
    }
    
    private double getRandom() {
		double high = 3;
		double low = 0.5;
		return Math.random() * (high - low) + low;
	}
    
    public void drawLine(){
    	
    	mTimer2 = new Runnable() {
            @Override
            public void run() {
                graph2LastXValue += 1d;
                graphviewSeries.appendData(new GraphViewData(graph2LastXValue, YValue), true, 1000);
                //graphviewSeries.appendData(new GraphViewData(graph2LastXValue, getRandom()), true, 40);
                GraphHandler.postDelayed(this, 200);
            }
        };
        GraphHandler.postDelayed(mTimer2, 1000);
    }
  
    public void drawCancle(){
    	
    	GraphHandler.removeCallbacks(mTimer2);
    }
    
    public void BLEsupportCheck(){
    	
    	if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Device doesn't have BLE support!", Toast.LENGTH_LONG).show();
			finish();

		}
    	
    }
    
    public void Connect_cancle(){
    	
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        
    }
    
    
    public void BLEScanning(){
    	
    	
    	final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    	mBluetoothAdapter = bluetoothManager.getAdapter();
   
    	
    	if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
    		
    	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    	}
    
    	
    	 mHandler.postDelayed(new Runnable() {
             @Override
             public void run() {
                 mScanning = false;
                 mBluetoothAdapter.stopLeScan(mLeScanCallback);
                 
             }
         }, SCAN_PERIOD);
         
         mScanning = true;
         mBluetoothAdapter.startLeScan(mLeScanCallback);
         
         
        
         mHandler.postDelayed(new Runnable() {
             @Override
             public void run() {
                 mScanning = false;
                 mBluetoothAdapter.stopLeScan(mLeScanCallback);
                 
             }
         }, SCAN_PERIOD);
         
         
         
         mHandler2.postDelayed(new Runnable() {
             @Override
             public void run() {
            	 Intent gattServiceIntent = new Intent (HRSActivity.this, BluetoothLeService.class);
                 bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                 registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                 
             }
         }, SCAN_PERIOD2);
         
        
         
        
         
 	
    }
    
    
    
    
    public BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			// TODO Auto-generated method stub

			runOnUiThread(new Runnable() {
		           @Override
		           public void run() {
		        	   mDeviceAddress = device.getAddress();
		        	   mDeviceName = device.getName();
		           }
		       });
			
		}
	};

	private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }
	
	private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }
	
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		
		
		
		String uuid = null;
		String unknownServiceString = "Unknown Service";
		String unknownCharaString = "Unknown Characteristic";
		
		String service_name;
		String characteristic_name;
		BluetoothGattCharacteristic characteristic = null;
		
		
		 for (BluetoothGattService gattService : gattServices) {
			 
			 uuid = gattService.getUuid().toString();

			 if(SampleGattAttributes.lookup(uuid, unknownServiceString).equals("Heart Rate Service")){
				 
			 	service_name = SampleGattAttributes.lookup(uuid, unknownServiceString);
			 	
			 }
			 
			 List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			 
			 for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				 
				 uuid = gattCharacteristic.getUuid().toString();
				 
				 if(SampleGattAttributes.lookup(uuid, unknownCharaString).equals("Heart Rate Measurement")){
					 
					 characteristic_name = SampleGattAttributes.lookup(uuid, unknownCharaString);
					 characteristic = gattCharacteristic;
					 
				 }
			 }
			  
		 }
		 
		 final int charaProp = characteristic.getProperties(); 
		 
		 if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
			
			 if (mNotifyCharacteristic != null) {
                 mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                 mNotifyCharacteristic = null;
             }
             mBluetoothLeService.readCharacteristic(characteristic);
			 
		 }
		 
		 if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
			 
			 
             mNotifyCharacteristic = characteristic;
             mBluetoothLeService.setCharacteristicNotification(characteristic, true);
         }
		 
		
	}
	
	
	private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
	
	
}
