package lean.termination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class TerminationService {
    private static final Logger logger = LoggerFactory.getLogger(TerminationService.class);

    private final Duration globalTimeout;
    private final Duration initiateTerminationTimeout;
    private final Duration blockingTerminationTimeout;

    private final List<Terminable> terminables = new CopyOnWriteArrayList<>();
    private final BlockingTerminableTerminator blockingTerminableTerminator = new BlockingTerminableTerminator();

    public TerminationService(Duration timeout) {
        this(timeout, timeout, timeout);
    }

    public TerminationService(Duration globalTimeout, Duration blockingTerminationTimeout, Duration initiateTerminationTimeout) {
        // validation
        if (globalTimeout.isNegative()) {
            throw new IllegalArgumentException("globalTimeout must be positive");
        }
        if (blockingTerminationTimeout.isNegative()) {
            throw new IllegalArgumentException("blockingTerminationTimeout must be positive");
        }
        if (initiateTerminationTimeout.isNegative()) {
            throw new IllegalArgumentException("initiateTerminationTimeout must be positive");
        }
        if (blockingTerminationTimeout.compareTo(globalTimeout) > 0) {
            throw new IllegalArgumentException("blockingTerminationTimeout must be less than globalTimeout");
        }
        if (initiateTerminationTimeout.compareTo(globalTimeout) > 0) {
            throw new IllegalArgumentException("initiateTerminationTimeout must be less than globalTimeout");
        }

        terminables.add(blockingTerminableTerminator);
        this.globalTimeout = globalTimeout;
        this.blockingTerminationTimeout = blockingTerminationTimeout;
        this.initiateTerminationTimeout = initiateTerminationTimeout;
    }

    public void register(Terminable terminable) {
        terminables.add(terminable);
    }

    public void register(BlockingTerminable blockingTerminable) {
        blockingTerminableTerminator.add(blockingTerminable);
    }

    public void terminate() {
        Instant start = Instant.now();
        Instant deadline = start.plus(globalTimeout);

        logger.debug("initiating terminations");
        terminables.forEach(this::initiateTermination);

        logger.debug("awaiting terminations");
        long terminatedCount = terminables.stream()
                .filter(terminable -> awaitTermination(terminable, deadline))
                .count();

        onGlobalTerminationCompleted(start, terminables.size(), terminatedCount);
    }

    protected void initiateTermination(Terminable terminable) {
        try {
            logger.debug("initiating termination of '{}'", terminable.name());

            Instant start = Instant.now();
            terminable.initiateTermination();
            Instant end = Instant.now();

            Duration duration = Duration.between(start, end);
            if (duration.compareTo(initiateTerminationTimeout) > 0) {
                onInitializeTerminationTimeout(terminable, duration, initiateTerminationTimeout);
            }
        } catch (Exception e) {
            onInitializeTerminationError(terminable, e);
        }
    }

    private static boolean awaitTermination(Terminable terminable, Instant deadline) {
        try {
            logger.debug("awaiting termination of '{}'", terminable.name());

            Duration remainingTimeout = Duration.between(deadline, Instant.now());
            boolean completed = terminable.awaitTermination(remainingTimeout);
            if (!completed) {
                onTerminationTimeout(terminable, deadline);
            }

            return completed;
        } catch (Exception e) {
            onTerminationError(terminable, e);
            return false;
        }
    }

    protected void onInitializeTerminationTimeout(Terminable terminable, Duration duration, Duration expected) {
        logger.error("initiating termination of '{}' took {} (up to {} is expected)", terminable.name(), duration, expected);
    }

    protected void onInitializeTerminationError(Terminable terminable, Exception exception) {
        logger.error("initiating termination of '{}' failed", terminable.name(), exception);
    }

    protected void onBlockingTerminationTimeout(BlockingTerminable blockingTerminable, Duration duration, Duration expected) {
        logger.error("blocking termination of '{}' took {} (up to {} is expected)", blockingTerminable.name(), duration, expected);
    }

    protected void onBlockingTerminationError(BlockingTerminable blockingTerminable, Exception exception) {
        logger.error("blocking termination of '{}' failed", blockingTerminable.name(), exception);
    }

    protected static void onTerminationTimeout(Terminable terminable, Instant deadline) {
        logger.error("termination of '{}' took {} extra", terminable.name(), Duration.between(deadline, Instant.now()));
    }

    protected static void onTerminationError(Terminable terminable, Exception exception) {
        logger.error("Error awaiting termination of '{}'", terminable.name(), exception);
    }

    protected static void onGlobalTerminationCompleted(Instant start, long terminablesCount, long terminatedCount) {
        if (terminablesCount != terminatedCount) {
            logger.error(
                    "{} terminables couldn't be terminated after {}",
                    terminablesCount - terminatedCount,
                    Duration.between(start, Instant.now()));
        } else {
            logger.info("all terminables have been successfully terminated within {}", Duration.between(start, Instant.now()));
        }
    }

    private class BlockingTerminableTerminator implements Terminable {
        private final ExecutorService executor = Executors.newCachedThreadPool();

        private final List<BlockingTerminable> blockingTerminables = new CopyOnWriteArrayList<>();

        private void add(BlockingTerminable blockingTerminable) {
            blockingTerminables.add(blockingTerminable);
        }

        @Override
        public void initiateTermination() {
            blockingTerminables.forEach(blockingTerminable -> {
                logger.debug("performing blocking termination of '{}'", blockingTerminable.name());
                executor.submit(() -> {
                    try {
                        Instant start = Instant.now();
                        blockingTerminable.close();
                        Instant end = Instant.now();

                        Duration duration = Duration.between(start, end);
                        if (duration.compareTo(blockingTerminationTimeout) > 0) {
                            onBlockingTerminationTimeout(blockingTerminable, duration, blockingTerminationTimeout);
                        }
                    } catch (Exception e) {
                        onBlockingTerminationError(blockingTerminable, e);
                    }
                });
            });
        }

        @Override
        public boolean awaitTermination(Duration timeout)
                throws InterruptedException {
            return executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
