package it.pintux.life.homesteadaddon.gateway;

public enum FlagDomain {
    WORLD_FLAGS("WorldFlags"),
    PLAYER_FLAGS("PlayerFlags"),
    CONTROL_FLAGS("ControlFlags");

    private final String className;

    FlagDomain(String className) {
        this.className = className;
    }

    public String className() {
        return className;
    }
}
