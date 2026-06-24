package com.java.beipuo.ae2virus.infection;

public enum T2VirusKind {
    FUSION("fusion"),
    SPECIALIZED("specialized"),
    SPECIAL_RESOURCE("special_resource");

    private final String serializedName;

    T2VirusKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public static T2VirusKind byName(String name) {
        for (T2VirusKind kind : values()) {
            if (kind.serializedName.equals(name)) {
                return kind;
            }
        }
        return SPECIAL_RESOURCE;
    }
}
