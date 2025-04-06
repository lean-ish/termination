package lean.termination;

import java.time.Duration;

public interface Terminable {

    /**
     * Initiates termination. Should return quickly.
     */
    void initiateTermination();

    /**
     * Awaits for shutdown completion.
     * @return {@code true} if terminated within timeout.
     */
    boolean awaitTermination(Duration timeout)
      throws InterruptedException;

    default String name() {
        return getClass().getSimpleName();
    }
}
