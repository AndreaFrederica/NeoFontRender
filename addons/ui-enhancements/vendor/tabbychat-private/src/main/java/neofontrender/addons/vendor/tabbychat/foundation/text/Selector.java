package neofontrender.addons.vendor.tabbychat.foundation.text;

public enum Selector {

    PLAYER('p'),
    ALL('a'),
    ENTITY('e'),
    RANDOM('r');

    private char id;

    Selector(char c) {
        this.id = c;
    }

    @Override
    public String toString() {
        return "@" + id;
    }
}
