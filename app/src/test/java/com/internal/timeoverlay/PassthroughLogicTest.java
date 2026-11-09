package com.internal.timeoverlay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PassthroughLogicTest {

    @Test
    public void lockedAlwaysPassthrough() {
        assertTrue(PassthroughPolicy.isEffectivePassthrough(true, 0, 31));
        assertTrue(PassthroughPolicy.isEffectivePassthrough(true, 0, 26));
    }

    @Test
    public void preAndroid12_unlockedNotForced() {
        assertFalse(PassthroughPolicy.isEffectivePassthrough(false, 100, 30));
        assertFalse(PassthroughPolicy.isEffectivePassthrough(false, 0, 30));
    }

    @Test
    public void android12_opacityCeiling() {
        assertFalse(PassthroughPolicy.isEffectivePassthrough(false, 10, 31));
        assertTrue(PassthroughPolicy.isEffectivePassthrough(false, 30, 31));
    }

    @Test
    public void backgroundOpacityMapping() {
        assertEquals(1.0f, PassthroughPolicy.backgroundObscuringOpacity(0), 0.001f);
        assertEquals(0.0f, PassthroughPolicy.backgroundObscuringOpacity(100), 0.001f);
        assertEquals(230f / 255f, PassthroughPolicy.backgroundObscuringOpacity(10), 0.0001f);
    }
}
