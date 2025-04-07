/*
 * Copyright (c) 2025 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.termination;

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
