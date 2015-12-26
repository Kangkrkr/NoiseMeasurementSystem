package noise.teamk.com.noisemeasurement;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import io.socket.SocketIO;

public class RoomListDialog extends Dialog {

    private ListView mToWhoList;

    private Context mContext;
    private ArrayAdapter<String> mArrayAdapter;
    private SocketIO mSocketIO;

    private Dialog preDialog = this;

    public RoomListDialog(Context context, ArrayAdapter<String> arrayAdapter, SocketIO socketIO) {
        // Dialog 배경을 투명 처리 해준다.
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mContext = context;
        mArrayAdapter = arrayAdapter;
        mSocketIO = socketIO;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.room_list_dialog);

        setLayout();
    }

    /*
    private void setClickListener(){
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMessage(mChatText.getText().toString());
                try {
                    JSONObject data = new JSONObject();
                    data.put("to", "JCNET-BT-6201").put("msg", getMessage());

                    mSocket.emit("send_msg", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                dismiss();
            }
        });

        mDissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }


    /*
     * Layout
     */
    private void setLayout() {
        mToWhoList = (ListView) findViewById(R.id.to_who_list);
        mToWhoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address[] = info.split("\n");

                CustomDialog cd = new CustomDialog(mContext, address[0], mSocketIO);
                cd.show();

                preDialog.dismiss();
            }
        });
        mToWhoList.setAdapter(mArrayAdapter);
    }
}