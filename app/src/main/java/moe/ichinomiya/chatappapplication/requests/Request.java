package moe.ichinomiya.chatappapplication.requests;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class Request {
    @JSONField(name = "type")
    private final String type;

    @JSONField(name = "data")
    private final Object data;

    public Request(ClientPacketTypes type, Object data) {
        this.type = type.getName();
        this.data = data;
    }
}
