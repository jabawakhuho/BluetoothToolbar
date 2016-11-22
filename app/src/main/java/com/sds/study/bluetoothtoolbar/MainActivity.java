package com.sds.study.bluetoothtoolbar;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    Toolbar toolbar;
    BluetoothAdapter bluetoothAdapter;
    static final int REQUEST_BLUETOOTH_ENABLE=1;
    static final int REQUEST_ACCESS_PERMISSION=2;
    Button bt_scan;
    BroadcastReceiver receiver;
    ListView listView;
    DeviceListAdapter deviceListAdapter;

    /*해당 디바이스에 접속하기 위해서는 소켓이 필요*/
    BluetoothSocket socket;//대화용 소켓!!
    String UUID="16d9f085-7837-426f-8213-0033be0b8705";//재우꺼~~
    Thread connectThread;//접속용 쓰레드
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        // 이 toolbar 를 앱바로 설정하자! 이 시점 부터 메뉴를 얹힌다거나, 네비게이션 버튼을 적용할수도 있는 시점..
        setSupportActionBar(toolbar);
        bt_scan =(Button)findViewById(R.id.bt_scan);
        listView =(ListView)findViewById(R.id.listView);
        deviceListAdapter = new DeviceListAdapter(this);
        listView.setAdapter(deviceListAdapter);//어뎁터 연결!!
        listView.setOnItemClickListener(this);//리스너 연결

        checkSupportBluetooth();
        requestActiveBluetooth();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /*시스템이 방송하는 정보가 action으로 들어옴!*/
                String action=intent.getAction();
                switch (action){
                    case BluetoothDevice.ACTION_FOUND :
                        BluetoothDevice bluetoothDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Toast.makeText(getApplicationContext(), bluetoothDevice.getName()+"발견", Toast.LENGTH_SHORT).show();
                        /*Adapter의 ArrayList에 추가하기*/
                        Device dto = new Device();
                        dto.setBluetoothDevice(bluetoothDevice);
                        dto.setName(bluetoothDevice.getName());
                        dto.setAddress(bluetoothDevice.getAddress());

                        boolean flag=true;
                        for(int i=0; i<deviceListAdapter.list.size();i++){
                            Device device = deviceListAdapter.list.get(i);
                           if(device.getAddress().equals(dto.getAddress())){
                              flag=false;
                           }
                        }

                        if(flag)deviceListAdapter.list.add(dto);

                        deviceListAdapter.notifyDataSetChanged();//갱신
                        break;
                }
            }
        };

    }

    /*엑티비티가 초기화 될때 메뉴 구성*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*메뉴 xml 을 inflation 시키자*/
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }
    /*액션 메뉴를 선택할때 호출되는 메서드*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(this, "당신이 선택한 메뉴는"+item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()){
            case R.id.m1 : ;break;
            case R.id.m2 :
                Intent intent = new Intent(this, MusicActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    /*디바이스가 블루투스를 지원하는지 여부 체크*/
    public void checkSupportBluetooth(){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            showMsg("안내","해당 기기는 블루투스 미지원 기기입니다.");
            finish();/*현재 activity를 close함*/
        }
    }
    /*유저에게 BT활성화 요청*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_BLUETOOTH_ENABLE :
                if(resultCode == Activity.RESULT_CANCELED){
                    showMsg("경고", "앱 사용하기 위해서는 Bluetooth를 활성화 해야 합니다.");
                    bt_scan.setEnabled(false);
                }
                break;
        }

    }

    public void requestActiveBluetooth(){
        Intent intent = new Intent();
        intent.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,REQUEST_BLUETOOTH_ENABLE);
    }

    /*검색 전, permission 확인 (CORESE~LOCATION)*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_ACCESS_PERMISSION:
                if(permissions.length>0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
                    showMsg("안내","위치 권한을 수락해주세요!!");
                }

        }
    }

    public void checkAccessPermission(){
        int accessPermission=ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(accessPermission== PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION
            },REQUEST_ACCESS_PERMISSION);
        }else{
            scanDevice();
        }
    }
    /*디바이스 검색*/
    public void scanDevice(){
         /*시스템에게 디바이스 스탬을 요청하되, 시스템이 알려주는 여러 글로벌 이벤트 중에서
        * 기기 발견 이벤트만 골라 받자!!(필터처리하자)*/
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //브로드 케스트 리시버 등록 시전!!!
        registerReceiver(receiver,filter);
        bluetoothAdapter.startDiscovery(); //검색 시작

    }

    public void btnClick(View view){
        switch (view.getId()){
            case R.id.bt_scan : checkAccessPermission();break;
            case R.id.bt_send : ; break;
        }
    }
    /*선택한 디바이스에 접속*/
    public void connectDevice(BluetoothDevice device){
        /*BT Device 검색을 멈춘다 why? 검색이 진행 중인 경우에는 연결 시도가 현저히 느려지고 실패할 가능성이 높음*/
        bluetoothAdapter.cancelDiscovery();
        /*브로드케스팅 그만 받기!!*/
        this.unregisterReceiver(receiver);
        try {
            /*socket 생성*/
            socket=device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));

        } catch (IOException e) {
            e.printStackTrace();
        }
        connectThread = new Thread(){

            public void run() {
                try {
                    socket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        connectThread.start();
    }

    /*원하는 디바이스를 선택한후 그디바이스를 이용하여 접속을 시도하겠음....*/
    public void onItemClick(AdapterView<?> adapterView, View item, int index, long id) {
        TextView txt_address=(TextView) item.findViewById(R.id.txt_address);
        String address = txt_address.getText().toString();

        for(int i=0; i< deviceListAdapter.list.size();i++){
            Device dto = deviceListAdapter.list.get(i);
            if(dto.getAddress().equals(address)){
              connectDevice(dto.getBluetoothDevice());
            }
        }
    }

    public void showMsg(String title, String msg){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(title).setMessage(msg).show();
    }



}
