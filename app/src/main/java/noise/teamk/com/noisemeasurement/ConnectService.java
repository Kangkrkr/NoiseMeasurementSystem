package noise.teamk.com.noisemeasurement;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

public class ConnectService extends Service implements IOCallback {

    private static SocketIO mSocket;
    private static Context mContext;
    private static Handler mHandler;
    private Vibrator vibe;

    private SoundPool sound_pool;
    private int sound_beep;
    private int message_arrive;
    private int knock_sound;

    public ConnectService() {
    }

    public ConnectService(Context context) {
        mContext = context;
        mHandler = new Handler();
        vibe = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);

        try {
            mSocket = new SocketIO("http://192.168.43.131:1337"); // IP 주소 설정
            mSocket.connect(this);

//            mContext.registerReceiver(new ConnectReceiver(mContext), new IntentFilter("com.action.message_received"));

            initSound();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static Context getContext() {
        return mContext;
    }

    public static SocketIO getSocket() {
        return mSocket;
    }

    public static Handler getHandler() {
        return mHandler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
//        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mSocket.disconnect();
    }

    @Override
    public void onDisconnect() {
        Log.i("통신종료", "무선통신 종료");
    }

    @Override
    public void onConnect() {
        Log.i("무선 통신 접속 성공", "무선 통신 접속 성공");
    }

    @Override
    public void onMessage(String s, IOAcknowledge ioAcknowledge) {

    }

    @Override
    public void onMessage(JSONObject jsonObject, IOAcknowledge ioAcknowledge) {

    }

    @Override
    public void on(String s, IOAcknowledge ioAcknowledge, final Object... objects) {
        final String event = s;
        final String message = objects[0].toString();

        if (event.equals("broadcast_msg")) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data = objects[0].toString();
                        JSONObject data_object = new JSONObject(data);

                        data = data_object.getString("msg");
//                        Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                        playMessageArriveSound();

                        Intent messsageIntent = new Intent("com.action.message_received");
                        messsageIntent.putExtra("message", data);

                        mContext.sendBroadcast(messsageIntent);

                        vibe.vibrate(500);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (event.equals("notify_alarm")) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    //         long[] pattern = {1000, 200, 1000, 2000, 1200};
                    //         vibe.vibrate(pattern, 0);
                    vibe.vibrate(500);
                }
            });
        }

        if (event.equals("do_vibe")) {
            Log.i("노크알림", "노크");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    playKnockSound();
                    vibe.vibrate(600);
                }
            });
        }
    }

    @Override
    public void onError(SocketIOException e) {
        e.printStackTrace();
    }

    private void initSound() {
        sound_pool = new SoundPool(20, AudioManager.STREAM_MUSIC, 0);
        sound_beep = sound_pool.load(mContext, R.raw.beep2, 1);
        message_arrive = sound_pool.load(mContext, R.raw.message_arrive, 1);
        knock_sound = sound_pool.load(mContext, R.raw.knock, 1);
    }

    public synchronized void playBeepSound() {
        sound_pool.play(sound_beep, 1f, 1f, 0, 0, 1f);
    }

    public synchronized void playMessageArriveSound() {
        sound_pool.play(message_arrive, 1f, 1f, 0, 0, 1f);
    }

    public synchronized void playKnockSound() {
        sound_pool.play(knock_sound, 1f, 1f, 0, 0, 1f);
    }
}
