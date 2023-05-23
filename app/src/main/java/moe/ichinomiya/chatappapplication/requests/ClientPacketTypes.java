package moe.ichinomiya.chatappapplication.requests;

import lombok.Getter;

public enum ClientPacketTypes {
    SendMessage("SEND_MESSAGE"),
    ChangeNickName("CHANGE_NICK_NAME"),
    GetUserProfile("GET_USER_PROFILE"),
    RecallMessage("RECALL_MESSAGE");

    @Getter
    private final String name;

    ClientPacketTypes(String name) {
        this.name = name;
    }
}
