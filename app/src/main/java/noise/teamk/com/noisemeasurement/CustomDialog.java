package noise.teamk.com.noisemeasurement;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.SocketIO;

public class CustomDialog extends Dialog {

    private BootstrapEditText mChatText;
    private BootstrapButton mSendButton;
    private BootstrapButton mDissButton;
    private BootstrapButton mVibeButton;
    private String mTitle;

    private Context mContext;
    private String mToWho;
    private SocketIO mSocket;

    private View.OnClickListener mLeftClickListener;
    private View.OnClickListener mRightClickListener;

    private String message = null;

    public CustomDialog(Context context, String toWho, SocketIO socket) {
        // Dialog 배경을 투명 처리 해준다.
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mContext = context;
        mToWho = toWho;
        mSocket = socket;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.custom_dialog);

        setLayout();
        setClickListener();
    }

    private void setClickListener() {
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMessage(mChatText.getText().toString());
                try {
                    JSONObject data = new JSONObject();
                    data.put("to", mToWho).put("msg", getMessage());

                    mSocket.emit("send_msg", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

//                dismiss();
            }
        });

        mDissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mVibeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectService.getSocket().emit("knock", mToWho);
            }
        });
    }

    /*
     * Layout
     */
    private void setLayout() {

        mChatText = (BootstrapEditText) findViewById(R.id.chat_text);
        mSendButton = (BootstrapButton) findViewById(R.id.send_chat_btn);
        mDissButton = (BootstrapButton) findViewById(R.id.dis_chat_btn);
        mVibeButton = (BootstrapButton) findViewById(R.id.vibe_btn);
    }

    public String getMessage() {
        return message;
    }

    private void setMessage(String message) {
        this.message = message;
    }
}