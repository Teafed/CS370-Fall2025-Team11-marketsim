package com.gui;

/**
 * Listener interface for startup events.
 */
public interface StartupListener {
    /**
     * Called when the user starts the application with a new profile.
     *
     * @param profileName The name of the profile.
     * @param balance     The initial balance.
     */
    void onStart(String profileName, double balance);
}
