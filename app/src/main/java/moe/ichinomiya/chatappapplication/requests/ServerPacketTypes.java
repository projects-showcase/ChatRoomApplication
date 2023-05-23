package moe.ichinomiya.chatappapplication.requests;

public enum ServerPacketTypes {
    UserProfile("USER_PROFILE"),
    Message("MESSAGE"),
    Notice("NOTICE"),
    RecallMessage("RECALL_MESSAGE");

    private final String name;

    ServerPacketTypes(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
