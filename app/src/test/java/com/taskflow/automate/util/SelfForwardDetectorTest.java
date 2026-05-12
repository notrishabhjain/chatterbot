package com.taskflow.automate.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelfForwardDetectorTest {

    @Test
    public void exactMatch_returnsTrue() {
        assertTrue(SelfForwardDetector.isSelfForwardedMessage("Alice", "Alice"));
    }

    @Test
    public void caseInsensitiveMatch_returnsTrue() {
        assertTrue(SelfForwardDetector.isSelfForwardedMessage("alice", "Alice"));
        assertTrue(SelfForwardDetector.isSelfForwardedMessage("ALICE", "alice"));
    }

    @Test
    public void matchWithWhitespace_returnsTrue() {
        assertTrue(SelfForwardDetector.isSelfForwardedMessage("  Alice  ", "Alice"));
        assertTrue(SelfForwardDetector.isSelfForwardedMessage("Alice", "  Alice  "));
    }

    @Test
    public void differentNames_returnsFalse() {
        assertFalse(SelfForwardDetector.isSelfForwardedMessage("Bob", "Alice"));
    }

    @Test
    public void nullSender_returnsFalse() {
        assertFalse(SelfForwardDetector.isSelfForwardedMessage(null, "Alice"));
    }

    @Test
    public void emptySender_returnsFalse() {
        assertFalse(SelfForwardDetector.isSelfForwardedMessage("", "Alice"));
    }

    @Test
    public void nullSelfName_returnsFalse() {
        assertFalse(SelfForwardDetector.isSelfForwardedMessage("Alice", null));
    }

    @Test
    public void emptySelfName_returnsFalse() {
        assertFalse(SelfForwardDetector.isSelfForwardedMessage("Alice", ""));
    }

    @Test
    public void bothNull_returnsFalse() {
        assertFalse(SelfForwardDetector.isSelfForwardedMessage(null, null));
    }

    @Test
    public void groupTitle_doesNotMatchSender() {
        // Simulates the bug scenario: in a group chat, the conversation title
        // ("Family Group") should NOT be used for self-forward detection.
        // Only the actual sender ("Alice") from EXTRA_MESSAGES should be compared.
        String groupTitle = "Family Group";
        String actualSender = "Alice";
        String selfName = "Alice";

        // Group title should not trigger self-forward
        assertFalse(SelfForwardDetector.isSelfForwardedMessage(groupTitle, selfName));
        // Actual sender should trigger self-forward
        assertTrue(SelfForwardDetector.isSelfForwardedMessage(actualSender, selfName));
    }

    @Test
    public void senderWithSpecialCharacters_matchesCorrectly() {
        assertTrue(SelfForwardDetector.isSelfForwardedMessage("Alice Smith", "Alice Smith"));
        assertFalse(SelfForwardDetector.isSelfForwardedMessage("Alice Smith", "Alice"));
    }

    @Test
    public void phoneNumberAsSender_matchesConfiguredNumber() {
        assertTrue(SelfForwardDetector.isSelfForwardedMessage("+1234567890", "+1234567890"));
        assertFalse(SelfForwardDetector.isSelfForwardedMessage("+1234567890", "Alice"));
    }
}
