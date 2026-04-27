package onebuilder;

final class RerootConfig {
    private final RerootMethod method;

    RerootConfig(RerootMethod method) {
        this.method = method == null ? RerootMethod.MAD : method;
    }

    static RerootConfig defaults() {
        return new RerootConfig(RerootMethod.MAD);
    }

    RerootMethod method() {
        return method;
    }
}
