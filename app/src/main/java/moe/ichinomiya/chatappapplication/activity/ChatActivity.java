package moe.ichinomiya.chatappapplication.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.WebSocket;
import com.ejlchina.okhttps.fastjson.FastjsonMsgConvertor;
import moe.ichinomiya.chatappapplication.Config;
import moe.ichinomiya.chatappapplication.R;
import moe.ichinomiya.chatappapplication.adapter.ChatAdapter;
import moe.ichinomiya.chatappapplication.data.ReceivedMessage;
import moe.ichinomiya.chatappapplication.data.UserData;
import moe.ichinomiya.chatappapplication.requests.ClientPacketTypes;
import moe.ichinomiya.chatappapplication.requests.Request;
import moe.ichinomiya.chatappapplication.requests.ServerPacketTypes;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatActivity extends AppCompatActivity {
    private static final HTTP http = HTTP.builder().baseUrl(Config.SERVER_ADDRESS.getValue()).addMsgConvertor(new FastjsonMsgConvertor()).build();
    public static UserData localUser;
    public static WebSocket webSocket;

    private final List<ReceivedMessage> messages = new CopyOnWriteArrayList<>();
    private final HashMap<Integer, UserData> userDataMap = new HashMap<>();

    private EditText inputText;
    private ChatAdapter chatAdapter;
    private Handler toastHandler;
    private Handler chatHandler;
    private ActivityResultLauncher<PickVisualMediaRequest> avatarPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        String token = intent.getStringExtra("token");

        toastHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Toast.makeText(ChatActivity.this, message.obj.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        chatHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 0) {
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                } else if (message.what == 1) {
                    int senderId = (int) message.obj;

                    messages.stream().filter(msg -> msg.getSenderId() == senderId).forEach(msg -> {
                        chatAdapter.notifyItemChanged(messages.indexOf(msg));
                    });
                } else if (message.what == 2) {
                    chatAdapter.notifyItemRemoved((int) message.obj);
                }
            }
        };

        avatarPicker = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                String filePath = "";
                String[] projection = {MediaStore.MediaColumns.DATA};
                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    cursor.moveToFirst();
                    filePath = cursor.getString(columnIndex);
                    cursor.close();
                }

                File file = new File(filePath);
                http.async("/user/updateAvatar").addBodyPara("token", token).addFilePara("file", file).post();
            }
        });

        // Create the websocket connection
        webSocket = http.webSocket("/ws/" + token).setOnOpen((WebSocket ws, HttpResult res) -> {
            toastHandler.sendMessage(Message.obtain(toastHandler, 0, "成功连接至服务器"));
        }).setOnMessage((WebSocket ws, WebSocket.Message msg) -> {
            try {
                JSONObject jsonObject = JSONObject.parseObject(msg.toString());
                boolean success = jsonObject.getBoolean("success");
                String type = jsonObject.getString("type");

                if (!jsonObject.containsKey("message")) {
                    String message = jsonObject.getString("message");
                    toastHandler.sendMessage(Message.obtain(toastHandler, 0, message));
                }

                if (success && type != null) {
                    if (type.equals(ServerPacketTypes.Message.getName())) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        String messageId = data.getString("id");
                        int senderId = data.getInteger("senderId");
                        String message = data.getString("message");

                        messages.add(new ReceivedMessage(messageId, senderId, message, new Date()));
                        chatHandler.sendMessage(Message.obtain(chatHandler, 0, null));

                        // If sender's profile doesn't exist in the map, request it from the server
                        if (!userDataMap.containsKey(senderId)) {
                            webSocket.send(JSON.toJSONString(new Request(ClientPacketTypes.GetUserProfile, new HashMap<String, Object>() {{
                                put("userId", senderId);
                            }})));
                        }
                    } else if (type.equals(ServerPacketTypes.UserProfile.getName())) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        UserData userData = JSON.parseObject(data.toString(), UserData.class);
                        RequestBuilder<Drawable> load = Glide.with(this).load(Config.SERVER_ADDRESS.getValue() + "/user/getAvatar?token=" + token + "&uid=" + userData.getUid() + "&time=" + System.currentTimeMillis());

                        userData.setAvatar(load);

                        if (userData.isLocal()) {
                            localUser = userData;
                        }

                        chatHandler.sendMessage(Message.obtain(chatHandler, 1, userData.getUid()));
                        userDataMap.put(userData.getUid(), userData);
                    } else if (type.equals(ServerPacketTypes.RecallMessage.getName())) {
                        String messageId = jsonObject.getString("data");

                        for (ReceivedMessage message : messages) {
                            if (message.getId().equals(messageId)) {
                                int index = messages.indexOf(message);
                                messages.remove(message);

                                chatHandler.sendMessage(Message.obtain(chatHandler, 2, index));
                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                toastHandler.sendMessage(Message.obtain(toastHandler, 0, "处理消息时出错"));
                finish();
            }
        }).setOnClosing((WebSocket ws, WebSocket.Close close) -> {
            toastHandler.sendMessage(Message.obtain(toastHandler, 0, "与服务器断开连接"));
            finish();
        }).listen();

        RecyclerView chatRecyclerView = findViewById(R.id.chat_recycler_view);
        inputText = findViewById(R.id.input_text);
        Button sendButton = findViewById(R.id.send_button);

        // Set up your RecyclerView here.
        chatAdapter = new ChatAdapter(messages, userDataMap);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        sendButton.setOnClickListener(v -> {
            String message = inputText.getText().toString();

            webSocket.send(JSON.toJSONString(new Request(ClientPacketTypes.SendMessage, new HashMap<String, Object>() {{
                put("message", message);
            }})));
            inputText.setText("");
        });

        Button changeNickName = findViewById(R.id.change_nick_button);
        changeNickName.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
            builder.setTitle("请输入新的昵称");

            // Set up the input
            final EditText input = new EditText(ChatActivity.this);
            // Specify the type of input expected
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("确定", (dialog, which) -> {
                String text = input.getText().toString();

                webSocket.send(JSON.toJSONString(new Request(ClientPacketTypes.ChangeNickName, new HashMap<String, Object>() {{
                    put("nickName", text);
                }})));
            });

            builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        Button changeAvatar = findViewById(R.id.change_avatar_button);
        changeAvatar.setOnClickListener(v -> avatarPicker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocket.close(1000, "退出聊天");
    }
}
