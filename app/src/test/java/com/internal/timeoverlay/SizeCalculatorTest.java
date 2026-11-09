package com.internal.timeoverlay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SizeCalculatorTest {

    @Test
    public void preset_dateMillis_standard() {
        SizeCalculator.Size size = SizeCalculator.presetSize(
                SizeModes.STANDARD, true, true);
        assertEquals(280, size.widthDp);
        assertEquals(36, size.heightDp);
    }

    @Test
    public void preset_heightsAlways_28_36_44() {
        assertEquals(28, SizeCalculator.presetSize(SizeModes.COMPACT, false, false).heightDp);
        assertEquals(36, SizeCalculator.presetSize(SizeModes.STANDARD, true, false).heightDp);
        assertEquals(44, SizeCalculator.presetSize(SizeModes.LARGE, false, true).heightDp);
    }

    @Test
    public void preset_widthsMatchTable() {
        assertEquals(240, SizeCalculator.presetSize(SizeModes.COMPACT, true, true).widthDp);
        assertEquals(208, SizeCalculator.presetSize(SizeModes.COMPACT, true, false).widthDp);
        assertEquals(144, SizeCalculator.presetSize(SizeModes.COMPACT, false, true).widthDp);
        assertEquals(96, SizeCalculator.presetSize(SizeModes.COMPACT, false, false).widthDp);

        assertEquals(320, SizeCalculator.presetSize(SizeModes.LARGE, true, true).widthDp);
        assertEquals(272, SizeCalculator.presetSize(SizeModes.LARGE, true, false).widthDp);
        assertEquals(192, SizeCalculator.presetSize(SizeModes.LARGE, false, true).widthDp);
        assertEquals(128, SizeCalculator.presetSize(SizeModes.LARGE, false, false).widthDp);
    }

    @Test
    public void customMinima() {
        assertEquals(200, SizeCalculator.minCustomSize(true, true).widthDp);
        assertEquals(168, SizeCalculator.minCustomSize(true, false).widthDp);
        assertEquals(112, SizeCalculator.minCustomSize(false, true).widthDp);
        assertEquals(80, SizeCalculator.minCustomSize(false, false).widthDp);
        assertEquals(24, SizeCalculator.minCustomSize(false, false).heightDp);
        assertEquals(28, SizeCalculator.minCustomSize(true, true).heightDp);
    }

    @Test
    public void validateCustom_bounds() {
        assertEquals("min", SizeCalculator.validateCustom(79, 24, false, false));
        assertEquals("max", SizeCalculator.validateCustom(601, 40, false, false));
        assertNull(SizeCalculator.validateCustom(80, 24, false, false));
        assertNull(SizeCalculator.validateCustom(600, 160, true, true));
        assertNotNull(SizeCalculator.validateCustom(199, 28, true, true));
    }
}
