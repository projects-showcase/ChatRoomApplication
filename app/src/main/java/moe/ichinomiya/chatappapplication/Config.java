package moe.ichinomiya.chatappapplication;

import lombok.Getter;

public enum Config {
    SERVER_ADDRESS("http://10.0.2.2:8080");

    @Getter
    private final String value;

    Config(String value) {
        this.value = value;
    }
}
