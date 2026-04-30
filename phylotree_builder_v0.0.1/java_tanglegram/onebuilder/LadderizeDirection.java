package onebuilder;

enum LadderizeDirection {
    UP("UP"),
    DOWN("DOWN");

    private final String jsonValue;

    LadderizeDirection(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    String jsonValue() {
        return jsonValue;
    }

    static LadderizeDirection fromJsonValue(String rawValue) {
        for (LadderizeDirection direction : values()) {
            if (direction.jsonValue.equalsIgnoreCase(String.valueOf(rawValue).trim())) {
                return direction;
            }
        }
        return UP;
    }

    @Override
    public String toString() {
        return jsonValue;
    }
}
