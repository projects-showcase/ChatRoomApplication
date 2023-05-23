package moe.ichinomiya.chatappapplication.data;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.RequestBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserData {
    int uid;
    String username;
    String nickname;
    boolean isLocal;
    RequestBuilder<Drawable> avatar;
}
