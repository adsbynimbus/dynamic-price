package com.adsbynimbus.dynamicprice

import kotlin.test.*

class AdSizeTest {

    @Test
    fun `When width and height at least 768, use the size as is`() {
        val case768x1024 = mapToFormat(768, 1024)
        assertEquals(expected = 768, actual = case768x1024.w)
        assertEquals(expected = 1024, actual = case768x1024.h)

        val case1024x768 = mapToFormat(1024, 768)
        assertEquals(expected = 1024, actual = case1024x768.w)
        assertEquals(expected = 768, actual = case1024x768.h)

        val case767x1024 = mapToFormat(767, 1024)
        assertNotEquals(illegal = 767, actual = case767x1024.w)
        assertNotEquals(illegal = 1024, actual = case767x1024.h)
    }

    @Test
    fun `When height less than 250 and width less than 728 use a 320x50 banner size`() {
        val case320x90 = mapToFormat(320, 90)
        assertEquals(expected = 320, actual = case320x90.w)
        assertEquals(expected = 50, actual = case320x90.h)

        val case728x90 = mapToFormat(728, 90)
        assertNotEquals(illegal = 320, actual = case728x90.w)
        assertNotEquals(illegal = 50, actual = case728x90.h)
    }

    @Test
    fun `When height less than 250 and width at least 728 use a 728x90 banner size`() {
        val case728x90 = mapToFormat(728, 90)
        assertEquals(expected = 728, actual = case728x90.w)
        assertEquals(expected = 90, actual = case728x90.h)

        val case728x250 = mapToFormat(728, 250)
        assertNotEquals(illegal = 728, actual = case728x250.w)
        assertNotEquals(illegal = 90, actual = case728x250.h)
    }

    @Test
    fun `When height at least 320 and width at least 480 use a 480x320 banner size`() {
        val case480x320 = mapToFormat(480, 320)
        assertEquals(expected = 480, actual = case480x320.w)
        assertEquals(expected = 320, actual = case480x320.h)

        val case479x320 = mapToFormat(479, 320)
        assertNotEquals(illegal = 480, actual = case479x320.w)
        assertNotEquals(illegal = 320, actual = case479x320.h)

        val case480x319 = mapToFormat(480, 319)
        assertNotEquals(illegal = 480, actual = case480x319.w)
        assertNotEquals(illegal = 320, actual = case480x319.h)
    }

    @Test
    fun `When height at least 480 and width at least 320 use a 320x480 banner size`() {
        val case320x480 = mapToFormat(320, 480)
        assertEquals(expected = 320, actual = case320x480.w)
        assertEquals(expected = 480, actual = case320x480.h)

        val case320x479 = mapToFormat(320, 479)
        assertNotEquals(illegal = 320, actual = case320x479.w)
        assertNotEquals(illegal = 480, actual = case320x479.h)

        val case319x480 = mapToFormat(319, 480)
        assertNotEquals(illegal = 320, actual = case319x480.w)
        assertNotEquals(illegal = 480, actual = case319x480.h)
    }

    @Test
    fun `When height at least 600 and width less than 320, use a 300x600 banner size`() {
        val case300x600 = mapToFormat(300, 600)
        assertEquals(expected = 300, actual = case300x600.w)
        assertEquals(expected = 600, actual = case300x600.h)

        val case320x600 = mapToFormat(320, 600)
        assertNotEquals(illegal = 300, actual = case320x600.w)
        assertNotEquals(illegal = 600, actual = case320x600.h)

        val case300x599 = mapToFormat(300, 599)
        assertEquals(expected = 300, actual = case300x599.w)
        assertEquals(expected = 250, actual = case300x599.h)
    }

    @Test
    fun `Other cases map to 300x250`() {
        val case300x250 = mapToFormat(300, 250)
        assertEquals(expected = 300, actual = case300x250.w)
        assertEquals(expected = 250, actual = case300x250.h)

        val case320x300 = mapToFormat(320, 300)
        assertEquals(expected = 300, actual = case320x300.w)
        assertEquals(expected = 250, actual = case320x300.h)

        val case480x275 = mapToFormat(480, 275)
        assertEquals(expected = 300, actual = case480x275.w)
        assertEquals(expected = 250, actual = case480x275.h)

        val case600x300 = mapToFormat(600, 300)
        assertEquals(expected = 300, actual = case600x300.w)
        assertEquals(expected = 250, actual = case600x300.h)
    }
}
