package noise.teamk.com.noisemeasurement;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageDialog extends ActionBarActivity {

    private TextView messageView;
    private BootstrapEditText chatTextView;
    private BootstrapButton responseButton;
    private BootstrapButton dismissButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_dialog);

        // 윈도우바 없애기..
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 액션바 없애기...
        getSupportActionBar().hide();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

        messageView = (TextView) findViewById(R.id.message_view);
        chatTextView = (BootstrapEditText) findViewById(R.id.chat_text_view);
        responseButton = (BootstrapButton) findViewById(R.id.response_message);
        dismissButton = (BootstrapButton) findViewById(R.id.dismiss_message);

        Intent intent = getIntent();
        messageView.setText(intent.getExtras().getString("message"));

        responseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageView.setVisibility(View.GONE);
                chatTextView.setVisibility(View.VISIBLE);

                responseButton.setText("보내기");
                responseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String responseMessage = messageView.getText().toString();
                        String toWho[] = responseMessage.split(" : ");

                        try {
                            JSONObject data = new JSONObject();
                            data.put("to", toWho[0]).put("msg", chatTextView.getText().toString());

                            ConnectService.getSocket().emit("send_msg", data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}