package onebuilder;

enum RerootMethod {
    MAD("MAD", "MAD"),
    ROOT_AT_MIDDLE_POINT("root-at-middle-point", "root-at-middle-point");

    private final String jsonValue;
    private final String label;

    RerootMethod(String jsonValue, String label) {
        this.jsonValue = jsonValue;
        this.label = label;
    }

    String jsonValue() {
        return jsonValue;
    }

    @Override
    public String toString() {
        return label;
    }
}
