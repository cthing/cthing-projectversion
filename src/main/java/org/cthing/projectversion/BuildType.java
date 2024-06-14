/*
 * Copyright 2024 C Thing Software
 * PDX-License-Identifier: Apache-2.0
 */

package org.cthing.projectversion;

/**
 * Specifies whether a build generates snapshot or release artifacts.
 */
public enum BuildType {
    /** Indicates that the build produces pre-release artifacts. */
    snapshot,

    /** Indicates that the build produces release artifacts. */
    release,
}
