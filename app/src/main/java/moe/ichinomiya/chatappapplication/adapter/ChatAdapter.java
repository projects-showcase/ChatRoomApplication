package moe.ichinomiya.chatappapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.fastjson.JSON;
import moe.ichinomiya.chatappapplication.R;
import moe.ichinomiya.chatappapplication.activity.ChatActivity;
import moe.ichinomiya.chatappapplication.data.ReceivedMessage;
import moe.ichinomiya.chatappapplication.data.UserData;
import moe.ichinomiya.chatappapplication.requests.ClientPacketTypes;
import moe.ichinomiya.chatappapplication.requests.Request;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ReceivedMessage> messages;
    private final Map<Integer, UserData> userMap;

    public ChatAdapter(List<ReceivedMessage> messages, Map<Integer, UserData> userMap) {
        this.messages = messages;
        this.userMap = userMap;
    }

    @NotNull
    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_message_layout, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ReceivedMessage message = messages.get(position);
        UserData userData = userMap.get(message.getSenderId());

        if (userData == null) {
            holder.nickNameText.setText("正在获取中");
        } else {
            holder.nickNameText.setText(userData.getNickname());
        }

        if (ChatActivity.localUser != null && message.getSenderId() == ChatActivity.localUser.getUid()) {
            holder.layout.setBackgroundColor(Color.parseColor("#A9FA7A"));
        }

        holder.messageTextView.setText(message.getMessage());
        holder.dateTimeText.setText(DateFormat.getDateTimeInstance().format(message.getDate()));
        if (userData != null) {
            userData.getAvatar().into(holder.avatarImage);
        }

        holder.itemView.setOnClickListener(click -> {
            if (userData != null) {
                // Create a dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                builder.setTitle("用户信息");
                builder.setMessage("UID: " + userData.getUid() + "\n" +
                        "用户名：" + userData.getUsername() + "\n" +
                        "昵称：" + userData.getNickname());

                builder.setPositiveButton("确定", (dialog, which) -> dialog.dismiss());
                if (ChatActivity.localUser != null && ChatActivity.localUser.getUid() == userData.getUid()) {
                    builder.setNegativeButton("撤回", (dialog, which) -> {
                        ChatActivity.webSocket.send(JSON.toJSONString(new Request(ClientPacketTypes.RecallMessage, new HashMap<String, Object>() {{
                            put("id", message.getId());
                        }})));
                    });
                }

                builder.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout layout;
        public ImageView avatarImage;
        public TextView nickNameText, dateTimeText, messageTextView;

        public ChatViewHolder(View itemView) {
            super(itemView);
            layout = (RelativeLayout) itemView;

            avatarImage = itemView.findViewById(R.id.avatar_image);
            nickNameText = itemView.findViewById(R.id.nick_name_text);
            dateTimeText = itemView.findViewById(R.id.date_time_text);
            messageTextView = itemView.findViewById(R.id.chat_message_text);
        }
    }
}