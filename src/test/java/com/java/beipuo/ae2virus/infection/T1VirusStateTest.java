package com.java.beipuo.ae2virus.infection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class T1VirusStateTest {
    @Test
    void levelThresholdsMatchValueDesign() {
        assertEquals(1, T1VirusState.levelForExperience(0L));
        assertEquals(1, T1VirusState.levelForExperience(999L));
        assertEquals(2, T1VirusState.levelForExperience(1_000L));
        assertEquals(3, T1VirusState.levelForExperience(16_000L));
        assertEquals(4, T1VirusState.levelForExperience(64_000L));
        assertEquals(5, T1VirusState.levelForExperience(256_000L));
    }

    @Test
    void blockedAmountAddsOneExperiencePerLockedItem() {
        T1VirusState state = new T1VirusState(null, 1L, 1L);

        assertTrue(state.addBlockedAmount(999L));
        assertEquals(1_000L, state.blockedAmount());
        assertEquals(1_000L, state.experience());
        assertEquals(2, state.level());

        assertFalse(state.addBlockedAmount(0L));
        assertEquals(1_000L, state.blockedAmount());
        assertEquals(1_000L, state.experience());
    }

    @Test
    void mergeKeepsHighestRestoredValuesForSameTarget() {
        T1VirusState state = new T1VirusState(null, 1L, 1L);

        assertTrue(state.merge(2_000L, 16_000L));
        assertEquals(2_000L, state.blockedAmount());
        assertEquals(16_000L, state.experience());
        assertEquals(3, state.level());

        assertFalse(state.merge(1L, 1L));
        assertEquals(2_000L, state.blockedAmount());
        assertEquals(16_000L, state.experience());
        assertEquals(3, state.level());
    }
}
