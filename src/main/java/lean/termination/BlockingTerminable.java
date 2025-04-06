package lean.termination;

public interface BlockingTerminable extends AutoCloseable {

    default String name() {
        return getClass().getSimpleName();
    }
}
