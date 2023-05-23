package moe.ichinomiya.chatappapplication.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.fastjson.FastjsonMsgConvertor;
import moe.ichinomiya.chatappapplication.Config;
import moe.ichinomiya.chatappapplication.R;

public class MainActivity extends AppCompatActivity {
    private static final HTTP http = HTTP
            .builder()
            .baseUrl(Config.SERVER_ADDRESS.getValue())
            .addMsgConvertor(new FastjsonMsgConvertor())
            .build();

    private EditText username;
    private EditText password;
    private Handler toastHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toastHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Toast.makeText(MainActivity.this, message.obj.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        Button login = findViewById(R.id.login);
        Button register = findViewById(R.id.register);

        login.setOnClickListener(v -> {
            String usernameText = username.getText().toString();
            String passwordText = password.getText().toString();

            http.async("/user/login")
                    .addBodyPara("username", usernameText)
                    .addBodyPara("password", passwordText)
                    .setOnResponse(response -> {
                        Mapper mapper = response.getBody().toMapper();

                        if (mapper.getBool("success")) {
                            String token = mapper.getString("data");

                            // Open the ChatActivity
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            intent.putExtra("token", token);
                            startActivity(intent);
                        } else {
                            toastHandler.sendMessage(Message.obtain(toastHandler, 0, mapper.getString("message")));
                        }
                    })
                    .post();
        });

        register.setOnClickListener(v -> {
            // Open the RegisterActivity
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}