package com.taskflow.automate.util;

/**
 * Utility for detecting self-forwarded WhatsApp messages.
 * The sender (extracted from EXTRA_MESSAGES) is compared against the user's configured
 * WhatsApp display name to determine if a message is a self-forward.
 */
public class SelfForwardDetector {

    /**
     * Checks if the given sender name matches the configured self-name, indicating
     * a self-forwarded message.
     *
     * @param sender   the message sender extracted from notification EXTRA_MESSAGES
     * @param selfName the user's configured WhatsApp display name
     * @return true if the sender matches the self-name (case-insensitive, trimmed)
     */
    public static boolean isSelfForwardedMessage(String sender, String selfName) {
        if (sender == null || sender.isEmpty()) {
            return false;
        }
        if (selfName == null || selfName.isEmpty()) {
            return false;
        }
        return sender.trim().equalsIgnoreCase(selfName.trim());
    }
}
