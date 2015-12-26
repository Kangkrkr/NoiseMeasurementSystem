package noise.teamk.com.noisemeasurement;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.socket.IOAcknowledge;
import kr.re.Dev.Bluetooth.BluetoothSerialClient;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UP_VALUE = 20;
    private static final int DOWN_VALUE = 1;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private TextView db_text = null;
    private LinearLayout sub_container = null;
    private TextView alert_box = null;
    private ProgressBar time_ticker = null;
    private TextView floor_name = null;
    private BluetoothAdapter mBTAdapter = null;
    private ArrayAdapter<String> mArrayAdapter = null;
    private ListView pair_list;
    private BluetoothSerialClient.BluetoothStreamingHandler mBSHandler;
    private BluetoothSerialClient cl = BluetoothSerialClient.getInstance();
    private ConnectService mConnectService;
    private BluetoothSocket mBTSocket;
    private RunThread runThread = null;
    // 블루투스 디바이스로부터 받아올 데이터를 저장하는 버퍼 (초기 사이즈는 2)
    private ArrayList<Integer> dataBuffer = new ArrayList<>();
    private AtomicInteger dB = new AtomicInteger(0);
    private boolean isAlarm = false;
    private boolean isBigger = false;
    private int progress = 0;
    private String room_name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        runThread = new RunThread();
        mConnectService = new ConnectService(this);
        Intent serviceIntent = new Intent("com.action.service_start");
        startService(serviceIntent);

        db_text = (TextView) findViewById(R.id.db_viewer);
        pair_list = (ListView) findViewById(R.id.pair_list);
        sub_container = (LinearLayout) findViewById(R.id.sub_container);
        alert_box = (TextView) findViewById(R.id.alert_box);
        time_ticker = (ProgressBar) findViewById(R.id.time_ticker);
        floor_name = (TextView) findViewById(R.id.floor_name);

        progress = time_ticker.getProgress();

        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBTAdapter == null) {
            // device does not support Bluetooth
        }


        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            addIntoArrayAdapter((ListView) findViewById(R.id.pair_list));
            ensureDiscoverable();
        }

        // 대화 시작 버튼을 누를시
        findViewById(R.id.chat_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                if(runThread == null){
                    activeThread();
                }
                */

                RoomListDialog rd = new RoomListDialog(ConnectService.getContext(), mArrayAdapter, ConnectService.getSocket());
                rd.show();
            }
        });

        // 측정 종료 버튼을 누를 시
        findViewById(R.id.dis_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (runThread != null) {
                    disableThread();
                    stopAnimation();

                    floor_name.setText("");
                }
            }
        });


        // 리스트 뷰의 각 아이템을 클릭시
        AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                mBTAdapter.cancelDiscovery();

                room_name = ((TextView) v).getText().toString().split("\n")[0];

                showCustomMessage(room_name + "에 연결 중 입니다..");

                String info = ((TextView) v).getText().toString();
                String address[] = info.split("\n");

                if (cl.connect(ConnectService.getContext(), mBTAdapter.getRemoteDevice(address[1]), mBSHandler)) {
                    Log.i("info", mBTAdapter.getRemoteDevice(address[1]).getName() + "에 접속됨.");

                    try {
                        mBTSocket = cl.getConnectedDevice().createRfcommSocketToServiceRecord(uuid);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        Log.i("error", "블루투스 장치에 연결할 수 없습니다..");
                    }
                }

                Log.i("address", address[1]);
            }
        };

        pair_list.setOnItemClickListener(mDeviceClickListener);

        mBSHandler = new BluetoothSerialClient.BluetoothStreamingHandler() {
            @Override
            public void onError(Exception e) {
                // 에러 발생. 연결이 종료된다.
                ConnectService.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("error", "블루투스 장치에 연결할 수 없습니다..");
                        showCustomMessage("블루투스 장치에 연결할 수 없습니다..");
                    }
                });
            }

            @Override
            public void onConnected() {
                if (runThread != null) {
                    disableThread();
                    activeThread();

                    ConnectService.getSocket().emit("client_connected", room_name);
                    floor_name.setText(room_name);
                } else {
                    activeThread();
                    floor_name.setText(room_name);
                }
            }

            @Override
            public void onDisconnected() {
                // 연결 종료 이벤트.
                if (runThread != null) {
                    runThread.setRunning(false);
                }
                Log.i("disconnected", "연결 종료 되었습니다.");
            }

            @Override
            public void onData(byte[] buffer, int length) {

                try {
                    ByteBuffer bb = ByteBuffer.wrap(buffer);

                    dB.set(Integer.parseInt(new String(bb.array()).replaceAll("[^0-9]", "")));   // 지금 들어온 문자열 값(받은 데이터중 숫자를 캐낸다)

                    findBiggerInBuffer();
                } catch (NumberFormatException e) {                                                  // 걸러낸 값중 숫자가 아닌값이 들어오게 되면 무시한다.
                    return;
                }
                /*
                try{
                    ByteBuffer bb = ByteBuffer.wrap(buffer);
                    String current = new String(bb.array()).replaceAll("[^0-9]", "");   // 지금 들어온 문자열 값(받은 데이터중 숫자만 캐낸다)


                    synchronized (data){
                        int integerCurrentValue = Integer.parseInt(current);                 // 지금 들어운 문자열을 정수로 바꾼 값
                        int integerPrevValue = Integer.parseInt(data);                      // 과거 저장돼있던 값의 정수값

                        mSBuilder.append(integerCurrentValue);

                        isUp = (integerCurrentValue >= integerPrevValue) ? true : false;

                        if(isUp && (integerCurrentValue >= 9)){  // 현재 데시벨이 증가중이고 값이 10 이상이라면
                            isDoubleLine = true;               // 현재 데시벨은 두자리값으로 진입했음을 알리기 위해 체크..
                        }

                        if(isDoubleLine){
                            if(mSBuilder.length() < 2){         // 두자리값 상태에서 현재 저장된 데시벨이 한자리 뿐이라면 ?
                                return;                         // 그냥 빠져나간다.
                            } else {
                                // 두 자리 이상이라면 ?
                                // 결과를 출력하고 그 값을 이전값을 담는 변수에 저장후,
                                // 스트링버퍼를 초기화한다.
                                data = mSBuilder.toString();
                                Log.i("결과 : ", mSBuilder.toString());
                                integerPrevValue = Integer.parseInt(mSBuilder.toString());
                                mSBuilder.setLength(0);
                                isDoubleLine = false;
                                return;
                            }
                        }

                        data = mSBuilder.toString();
                        Log.i("결과 : ", mSBuilder.toString());
                        integerPrevValue = Integer.parseInt(mSBuilder.toString());
                        mSBuilder.setLength(0);
                    }

                } catch(NumberFormatException e){
                    Log.i("error", "숫자가 아닙니다.");
                }*/

                /*
                if(s.toString().contains("=")){

                    String workedData[] = s.split("=");
                    if(workedData[0].equals("Mic")){
                        StringBuffer sb = new StringBuffer();
                        for(int i=0; i<workedData[1].length(); i++){
                            if((workedData[1].charAt(i) >= '0') && (workedData[1].charAt(i) <= '9')){
                                sb.append(workedData[1].charAt(i));
                            }else{
                                break;
                            }
                        }
                        data = sb.toString();
                    }else {
                        return;
                    }
                }else{
                    return;
                }
                */
            }
        };
    }

    private void findBiggerInBuffer() {
        dataBuffer.add(dB.get());
//        Log.i("get_data", dB.get() + "");

        if (dataBuffer.size() == 2) {
            isBigger = (dataBuffer.get(0) <= dataBuffer.get(1));
//            Log.i("isBigger", String.valueOf(isBigger));

            dataBuffer.clear();
//            Log.i("clear", "cleared");
        }
    }

    private void stopAnimation() {
        final Animation top_to_bottom = AnimationUtils.loadAnimation(ConnectService.getContext(), R.anim.top_to_bottom);
        final Animation bottom_to_top = AnimationUtils.loadAnimation(ConnectService.getContext(), R.anim.bottom_to_top);
        ConnectService.getHandler().post(new Runnable() {
            @Override
            public void run() {

                sub_container.setAnimation(bottom_to_top);
                sub_container.startAnimation(bottom_to_top);
                sub_container.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sub_container.setVisibility(View.GONE);

                        pair_list.setVisibility(View.VISIBLE);
                        pair_list.setAnimation(top_to_bottom);
                        pair_list.startAnimation(top_to_bottom);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                pair_list.startAnimation(bottom_to_top);
            }
        });
    }

    private void disableThread() {
        runThread.setRunning(false);
        runThread = null;
    }

    private void activeThread() {
        runThread = new RunThread();
        runThread.setRunning(true);
        runThread.start();
    }

    private void showCustomMessage(final String message) {
        ConnectService.getHandler().post(new Runnable() {
            @Override
            public void run() {
                Animation LtoR = AnimationUtils.loadAnimation(ConnectService.getContext(), R.anim.left_to_right);
                alert_box.setText(message);
                alert_box.setVisibility(View.VISIBLE);
                alert_box.setAnimation(LtoR);
                alert_box.startAnimation(LtoR);
                alert_box.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        alert_box.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        });
    }

    public void addIntoArrayAdapter(ListView list) {
        Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    String tokenizer = device.getName();
                    if (tokenizer.contains("JCNET")) {
                        String realRoomName = (device.getName().contains("1297")) ? "2층" : "1층";
                        mArrayAdapter.add(realRoomName + "\n" + device.getAddress());
                    }
                    Log.i("info", "페어링 중인 디바이스 : " + device.getName() + "\n" + device.getAddress());
                } else {
                    Log.i("info", "페어링 되지 않은 디바이스 : " + device.getName() + "\n" + device.getAddress());
                }
            }

            list.setAdapter(mArrayAdapter);

            Animation top_to_bottom = AnimationUtils.loadAnimation(ConnectService.getContext(), R.anim.top_to_bottom);
            pair_list.setAnimation(top_to_bottom);
            pair_list.startAnimation(top_to_bottom);
        }
    }

    public void ensureDiscoverable() {
        if (mBTAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    addIntoArrayAdapter(pair_list);
                    ensureDiscoverable();
                }
        }
    }

    @Override
    protected void onDestroy() {

        if (runThread != null) {
            disableThread();
        }

        if (mBTAdapter != null) {
            mBTAdapter.disable();
            mBTAdapter.cancelDiscovery();
        }

        if (mBTSocket != null) {
            try {
                mBTSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }

    class RunThread extends Thread {

        private static final int MAX_DB = 55;
        private static final int MEASURE_DELAY = 200;
        private AtomicBoolean isRunning = new AtomicBoolean(false); // 스레드 동작을 유지시켜줄 변수,

        @Override
        public void run() {

            final Animation top_to_bottom = AnimationUtils.loadAnimation(ConnectService.getContext(), R.anim.top_to_bottom);
            final Animation bottom_to_top = AnimationUtils.loadAnimation(ConnectService.getContext(), R.anim.bottom_to_top);
            ConnectService.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    startAnimation(top_to_bottom, bottom_to_top);
                }
            });

//            initSound();

            while (isRunning.get()) {
                try {

                    Thread.sleep(MEASURE_DELAY);

                    ConnectService.getHandler().post(new Runnable() {
                        @Override
                        public void run() {

                            if (progress > 100) {
                                progress = 100;
                            }

                            if (progress < 0) {
                                progress = 0;
                            }

                            if ((dB.get() >= MAX_DB)) {
                                if (isBigger) {
                                    if (time_ticker.getProgress() >= time_ticker.getMax()) {
                                        ConnectService.getHandler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                ConnectService.getSocket().emit("noise_alarm", new IOAcknowledge() {
                                                    @Override
                                                    public void ack(Object... objects) {
                                                        final String message = objects[0].toString();
                                                        try {
                                                            JSONObject data = new JSONObject();
                                                            data.put("to", "ALL").put("msg", message);
                                                            ConnectService.getSocket().emit("send_msg", data);
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }, room_name);
                                            }
                                        }, 500);
                                    }
                                    time_ticker.setProgress(progress += UP_VALUE);
                                    mConnectService.playBeepSound();
                                } else {
                                    time_ticker.setProgress(progress -= DOWN_VALUE);
                                }
                            } else {
                                time_ticker.setProgress(progress -= DOWN_VALUE);
                            }

                            db_text.setText(String.valueOf(dB.get()));
//                            time_ticker.setProgress(progress+=1);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void startAnimation(Animation top_to_bottom, Animation bottom_to_top) {
            sub_container.setVisibility(View.VISIBLE);
            sub_container.setAnimation(top_to_bottom);
            sub_container.startAnimation(top_to_bottom);

            pair_list.setAnimation(bottom_to_top);
            pair_list.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    pair_list.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            pair_list.startAnimation(bottom_to_top);
        }

        public boolean getRunning() {
            return isRunning.get();
        }

        public void setRunning(boolean bool) {
            isRunning.set(bool);
        }

        public int getdB() {
            return dB.get();
        }

        public void setdB(int db) {
            dB.set(db);
        }
    }
}