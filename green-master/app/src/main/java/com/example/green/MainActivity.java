package com.example.green;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private final String TAG = "BluetoothList";
    IntentFilter intentFilter;
    ListView listView;
    BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayAdapter<String> adapter;
    private final List<String> bluetoothList = new ArrayList<>();
    Handler uiHandler;

    @SuppressLint("HandlerLeak")
    @Override
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button turnOn = (Button) findViewById(R.id.turn_on);
        Button turnOff = (Button) findViewById(R.id.turn_off);
        Button search = (Button) findViewById(R.id.search_button);

        turnOn.setOnClickListener(this);
        turnOff.setOnClickListener(this);
        search.setOnClickListener(this);
        //安卓6以后使用蓝牙要用定位权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
        //蓝牙广播注册
        registerBluetooth();
        //listView布局加载
        //参数： android.R.layout.simple_list_item_1:单行的文本
        //      android.R.layout.simple_list_item_checked：带选择标识的
        //      simple_list_item_multiple_choice：带复选框
        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, bluetoothList);
        listView = (ListView) findViewById(R.id.search_result);
        listView.setAdapter(adapter);
        //listView点击函数
        listClick();
        TextView showMessage=(TextView) findViewById(R.id.getMessage);
        uiHandler=new Handler(){
            public void handleMessage(Message msg){
                Bundle bundle=msg.getData();
                String data=bundle.getString("BtData");
                showMessage.append(data);
            }
        };


    }

    //listView点击的实现
    void listClick() {
        listView.setOnItemClickListener((adapterView, view, i, l) -> {

        });
    }

    //广播注册的实现
    void registerBluetooth() {
        // 设置广播信息过滤
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //注册广播
        registerReceiver(bluetoothReceiver, intentFilter);
    }

    //button点击的实现
    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
            if(view.getId()== R.id.turn_on) {
                if (myBluetoothAdapter == null) {
                    Toast.makeText(this, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
                }
                if (!myBluetoothAdapter.isEnabled()) {

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }
                    myBluetoothAdapter.enable();//打开蓝牙适配器
                }
                //开启被其它蓝牙设备发现的功能
                if (myBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    //设置为一直开启
                    i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                    startActivity(i);
                }
            }

            if(view.getId()==R.id.turn_off){
                bluetoothList.clear();
                adapter.notifyDataSetChanged();
                myBluetoothAdapter.disable();
                Toast.makeText(this,"蓝牙已关闭",Toast.LENGTH_LONG).show();
            }

            if( view.getId()== R.id.search_button){
                Log.d(TAG, "有点击search_button");
                if ( !bluetoothList.isEmpty())
                    bluetoothList.clear();
                //如果当前在搜索，就先取消搜索
                if (myBluetoothAdapter.isDiscovering()) {
                    myBluetoothAdapter.cancelDiscovery();
                }
                //开启搜索
                myBluetoothAdapter.startDiscovery();
                }
        }


    //蓝牙开始搜索的回调
    //广播
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String myPhoneMacAddress = "";//手机的mac，可根据具体的情况修改
            String HC_05 = "00:20:12:08:B2:C9";//硬件的mac
            String action = intent.getAction();
            Log.d(TAG, "有回调");
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    //配对
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }
                    if (device.getAddress().contains(HC_05))//HC-05设备如果有多个，第一个搜到的那个会被尝试。
                    {
                        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                            Log.e("ywq", "attemp to bond:" + "[" + device.getName() + "]");
                            try {
                                //调用createBond方法
//                                createBond(device.getClass(), device);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    device.createBond();
                                }
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                    //权限检查
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    }

                    //已匹配的设备
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {


                        bluetoothList.add(device.getName() + "\n" + device.getAddress() + "(已配对设备)");
                        adapter.notifyDataSetChanged();

                        mClientLinkThread clientThread = new mClientLinkThread(device);
                        clientThread.start();//启动线程
                    } else {
                        bluetoothList.add(device.getName() + "\n" + device.getAddress() + "(未配对设备)");
                        adapter.notifyDataSetChanged();
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Toast.makeText(MainActivity.this, "开始搜索", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(MainActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    //连接蓝牙
    private class mClientLinkThread extends Thread {//连接与接收数据线程类
        private final BluetoothDevice device;

        public mClientLinkThread(BluetoothDevice device) {
            this.device = device;
        }

        BluetoothSocket socket = null;

        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
            //关闭发现
            myBluetoothAdapter.cancelDiscovery();
            try {

                String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

                socket.connect();

                runOnUiThread(() -> {//更新ui显示已连接
                    Toast.makeText(MainActivity.this, "已连接", Toast.LENGTH_SHORT).show();
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }
                    bluetoothList.remove(bluetoothList.size() - 1);
                    bluetoothList.add(device.getName() + "\n" + device.getAddress() + "(已连接设备)");
                    adapter.notifyDataSetChanged();
                });
                Log.d("提示", "socket调用connect");

                //获取输入流
                InputStream inputStream = socket.getInputStream();

                final byte[] bytes = new byte[1024];
                int count;
//                Handler uiHandler=new Handler();
                Bundle data = new Bundle();
                while (true) {
                    try {
                        int availableBytes = inputStream.available();//获取流中尚未读取的字节的数量
                        if (availableBytes > 0) {
                            count = inputStream.read(bytes);//返回读入的字节数，将输入流读到字节数组中
                            byte[] contents = new byte[count];
                            for (int i = 0; i < contents.length; i++) {
                                contents[i] = bytes[i];
                            }
                            String readMessage = toHexString(contents);//字节数据转十六进制

                            data.clear();//清空内容，等待装入数据
                            /*蓝牙发送来的数据开头：
                            * 11是深度标志
                            * 22是重量标志
                            * 33是塑料瓶标志
                            * 44是玻璃瓶标志
                            * 55是易拉罐标志
                            * 66是无法识别标志*/
                            char firstNum=readMessage.charAt(0);//标识数据
                            Log.d("数据标记：",firstNum+"");
                            if(firstNum=='1'){
                                data.putString("BtData","\n");
                            }else if(firstNum=='2') {
                                //dealHexString(readMessage)
                                data.putString("BtData","重量为："+dealHexString(readMessage)+"g\n" );
                            }else if(firstNum=='3'){
                                data.putString("BtData","种类为塑料瓶\n");
                            }else if(firstNum=='4'){
                                data.putString("BtData","种类为玻璃瓶\n");
                            } else if (firstNum=='5') {
                                data.putString("BtData","种类为易拉罐\n");
                            } else if (firstNum=='6') {
                                data.putString("BtData","种类未识别出来\n");
                            }
                            Message msg = Message.obtain();
                            msg.setData(data);

                            uiHandler.sendMessage(msg);//用于ui显示数据

                        }
                    } catch (IOException e) {
                        Log.e(TAG, "disconnected", e);
                        socket.close();
                        break;
                    }
                }

            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Log.e("异常:", "socket无法close");
                }
                e.printStackTrace();
            }
        }

        //转换成十进制
        int number(char num) {
            switch (num) {
                case 'a':
                    return 10;

                case 'b':
                    return 11;

                case 'c':
                    return 12;

                case 'd':
                    return 13;

                case 'e':
                    return 14;

                case 'f':
                    return 15;

                case '0':
                    return 0;
                default:
                    return num - '0';
            }
        }

         String dealHexString(String str) {
            StringBuilder box = new StringBuilder();
            char[] ch = str.toCharArray();
            char high = '0', low;
            int i;
            int count = 2;// 十六进制数组的长度
            while (count < ch.length) {

                if (count % 2 == 1) {
                    low = ch[count];
                    box.append(low);
                    box.append(high);

                } else {
                    high = ch[count];

                }
                count++;
            }
            String stander = box.toString();//
            char[] ch2 = stander.toCharArray();
            i = ch2.length - 1;
            do {// 清除后面的零
                if (ch2[i] != '0')
                    break;
                i -= 1;
            } while (i > 0);
            double result = number(ch2[0]);
            for (int i2 = 1; i2 <= i; i2++) {
                result += Math.pow(16, i2) * number(ch2[i2]);

            }
            return result + "";
        }

        //处理比特数组转换成十六进制
        char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        String toHexString(byte[] bytes) {
            StringBuilder str = new StringBuilder();
            int num;
            for (byte i : bytes) {
                num = i < 0 ? 256 + i : i;
                str.append(HEX_CHAR[num / 16]).append(HEX_CHAR[num % 16]);
            }
            return str.toString();
        }


    }

    //注册销毁
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
    }

}
