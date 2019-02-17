package com.example.naddi.wi_fip2p;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION =1 ;
    Button btnOnOff, btnDiscover, btnSend, button, inviatempo;
    ListView listView;
    TextView read_msg_box, connectionsStatus;
    EditText writeMsg;
    WifiManager wifimanager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChanel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> peers= new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    static  final int MESSAGE_READ =1;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive ;
    int mexsize=0;
    String payload;
    long inizioDisctime;
    long fineDisc;

    long gruppoFormato;
    boolean primo= true;
    long end = 0;



    public void setPayload(){
        int x=0;
        Toast.makeText(getApplicationContext(),"creazione payload",Toast.LENGTH_SHORT).show();

        StringBuilder stringBuilder = new StringBuilder();
        while (x< 1048960 *10){
            stringBuilder.append("a");
            x=x+1;
        }
        payload=stringBuilder.toString();
        Toast.makeText(getApplicationContext(),"payload creato",Toast.LENGTH_SHORT).show();

    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
        setPayload();
       System.out.println(payload);
       System.out.println(payload.length());



    }



    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String (readBuff,0,msg.arg1);


                    if(tempMsg.startsWith("tempo:")){
                       String a = tempMsg.subSequence(6,19).toString();
                       System.out.println(a);

                       long x = Long.parseLong(a);

                       TextView K = findViewById(R.id.ricezione);
                       String num=  K.getText().toString().subSequence(15,28).toString();
                       System.out.println(num);
                       long inizio= Long.parseLong(num);
                        System.out.println("------------------------------------------------------------");
                       System.out.println(x-inizio);
                       double kk=((x-inizio)/1000);
                        System.out.println(kk);
                        System.out.println("------------------------------------------------------------");
                        TextView s = findViewById(R.id.tempmex);

                        s.setText(s.getText().toString()+" "+kk);



                    }else{

                        end = System.currentTimeMillis();
                        // TextView dim = findViewById(R.id.dim);
                        mexsize=mexsize+tempMsg.length();
                        //dim.setText(dimm);
                        TextView a= findViewById(R.id.ricezione);
                        a.setText("ricezione mex: "+ end);


                    }




                    break;
            }
            return true;
        }
    });

    private void exqListener() {
        btnOnOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wifimanager.isWifiEnabled()){
                    wifimanager.setWifiEnabled(false);
                    btnOnOff.setText("WIFI ACCESSO");
                }else{
                    wifimanager.setWifiEnabled(true);
                    btnOnOff.setText("WIFI SPENTO");
                }
            }
        });


        inviatempo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                TextView a= findViewById(R.id.invio);


                String mex="tempo"+a.getText().toString().subSequence(9,23);
                System.out.println(mex);
                try{
                    sendReceive.write(mex.getBytes());
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(),"invio fallito",Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }


            }
        });



        btnDiscover.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                requ();



                mManager.discoverPeers(mChanel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                        inizioDisctime = System.currentTimeMillis();
                        connectionsStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionsStatus.setText("Discovery start fail");
                    }
                });
            }
        });

        gruppoFormato = System.currentTimeMillis();


//----------------------RICHIESTA CONNESSIONE-------------------------------------------
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                mManager.connect(mChanel,config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"connected to"+device.deviceName+
                                "  mac"+device.deviceAddress,Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"not connected",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(mexsize);
                button.setText("dd"+mexsize);
            }
        });


        btnSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                long start = System.currentTimeMillis();
                TextView a= findViewById(R.id.invio);
                a.setText("invio mex:"+ start);

                String msg = payload;
                try{
                    sendReceive.write(msg.getBytes());
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(),"invio fallito",Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }





    private void initialWork() {
        button =findViewById(R.id.size);
        btnOnOff = findViewById(R.id.onOff);
        btnDiscover = findViewById(R.id.discover);
        btnSend = findViewById(R.id.sendButton);
        listView =  findViewById(R.id.peerListView);
        connectionsStatus= findViewById(R.id.connectionStatus);
        writeMsg = findViewById(R.id.writeMsg);
        wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChanel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new WifiDirectBroadcastReceiver(mManager,mChanel,this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        inviatempo = findViewById(R.id.inviatempo);

    }

    WifiP2pManager.PeerListListener peerListListener= new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray= new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;

                for(WifiP2pDevice device : peerList.getDeviceList()){
                    if ((device.deviceAddress.equals("4a:2c:a0:1e:3e:c5") || device.deviceAddress.equals("02:0a:f5:31:43:00" )) && primo){
                        fineDisc = System.currentTimeMillis();


                        double a=(fineDisc-inizioDisctime)/1000;

                        TextView k =findViewById(R.id.discpeer);

                        k.setText("tempo discover peer: "+a);
                        primo = false;
                    }


                    deviceNameArray[index]= device.deviceAddress+"  "+device.deviceName;
                    deviceArray[index]=device;
                    index++;

                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String> (getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);

            }
            if (peers.size()==0){
                Toast.makeText(getApplicationContext(),"no device found",Toast.LENGTH_SHORT).show();
            }

        }
    };


   //---------------------------------------LISTENER DELLA CONNESSIONE----------------------------------------------------------
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnwerAndres = wifiP2pInfo.groupOwnerAddress;

            double aa=0;
            if (wifiP2pInfo.groupFormed){
               aa = ( System.currentTimeMillis() - gruppoFormato)/1000;

            }

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionsStatus.setText("host "+aa);

                serverClass=new ServerClass();
                serverClass.start();
               // serverClass.run();


            }else if (wifiP2pInfo.groupFormed){
                connectionsStatus.setText("client "+aa);
                clientClass = new ClientClass(groupOwnwerAndres);
                clientClass.start();
               // clientClass.run();
            }
        }
    };




    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0x12345) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            //getWifi();
        }
    }
    @TargetApi(23)
    public boolean requ(){
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);                    // Get the result in onRequestPermissionsResult(int, String[], int[])
        } else {
            return true;
        }
        return false;
    }




    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);

    }

 //------------------------SERVER-----------------------------
    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;


        @Override
        public void run(){
            try {
                serverSocket= new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceive =new SendReceive(socket);
                sendReceive.start();
            }catch (IOException e){
                e.printStackTrace();
            }}
    }
//------------------------SENDRECEIVE-----------------------------
    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            socket = skt;
            try{
                inputStream= socket.getInputStream();
                outputStream = socket.getOutputStream();
            }catch (IOException e){
               e.printStackTrace();
            }
         }
        @Override
        public void run(){
            byte[] buffer = new byte[2048];
            int bytes ;

            while(socket != null){
                try {
                    bytes = inputStream.read(buffer);
                    if(bytes > 0){
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {

                }
            }

        }

        public void write(final byte[] bytes) {
            new Thread(new Runnable(){
                @Override
                public void run() {
                    try {

    //invio il mex alla socket di output



                        outputStream.write(bytes);


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }



//------------------------CLIENT-----------------------------
    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;

        @Override
        public void run() {
            try{
                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                sendReceive =new SendReceive(socket);
                sendReceive.start();

                }catch (IOException e){
                    e.printStackTrace();
            }
        }
        public ClientClass(InetAddress hostAddress){
            hostAdd = hostAddress.getHostAddress();
            socket= new Socket();
        }
    }








    public void connectAndSendMessage(final String mac){

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = mac;

        mManager.connect(mChanel,config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"connected to"+mac,Toast.LENGTH_SHORT).show();





            }
            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"not connected",Toast.LENGTH_SHORT).show();
            }
        });




    }
}
