package moe.ichinomiya.chatappapplication.data;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.RequestBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class ReceivedMessage {
    private String id;
    private int senderId;
    private String message;
    private Date date;
}
