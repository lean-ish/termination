/*
 * Copyright (c) 2025 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.termination;

public interface BlockingTerminable extends AutoCloseable {

    default String name() {
        return getClass().getSimpleName();
    }
}
