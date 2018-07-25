package io.dico.dicore.command;

/**
 * Override policies for registering to the command map
 */
public enum EOverridePolicy {
    OVERRIDE_ALL,
    MAIN_KEY_ONLY,
    MAIN_AND_FALLBACK,
    FALLBACK_ONLY,
    OVERRIDE_NONE
}
