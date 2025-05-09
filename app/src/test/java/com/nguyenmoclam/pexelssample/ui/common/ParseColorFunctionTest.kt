package com.nguyenmoclam.pexelssample.ui.common

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseColorFunctionTest {

    @Test
    fun `parseColor with valid hex string (#FF0000) returns Red`() {
        val hex = "#FF0000" // Red
        val expectedColor = Color(0xFFFF0000.toInt())
        assertEquals(expectedColor, parseColor(hex))
    }

    @Test
    fun `parseColor with valid hex string (00FF00) returns Green`() {
        val hex = "00FF00" // Green
        val expectedColor = Color(0xFF00FF00.toInt())
        assertEquals(expectedColor, parseColor(hex))
    }

    @Test
    fun `parseColor with valid hex string (#800000FF) returns Blue with alpha`() {
        val hexWithAlpha = "#800000FF" // Blue with ~50% alpha
        val expectedColor = Color(0x800000FF.toInt())
        assertEquals(expectedColor, parseColor(hexWithAlpha))
    }

    @Test
    fun `parseColor with valid hex string (8000FF00) returns Green with alpha`() {
        val hexWithAlpha = "8000FF00" // Green with ~50% alpha
        val expectedColor = Color(0x8000FF00.toInt())
        assertEquals(expectedColor, parseColor(hexWithAlpha))
    }

    @Test
    fun `parseColor with short hex string (e_g_, #RGB) returns LightGray (not supported by toColorInt)`() {
        assertEquals(Color.LightGray, parseColor("#F0F"))
        assertEquals(Color.LightGray, parseColor("F0F"))
        assertEquals(Color.LightGray, parseColor("#ABC"))
    }

    @Test
    fun `parseColor with invalid hex characters returns LightGray`() {
        assertEquals(Color.LightGray, parseColor("#GGHHII"))
        assertEquals(Color.LightGray, parseColor("FF00G0"))
    }

    @Test
    fun `parseColor with invalid hex length (too short) returns LightGray`() {
        assertEquals(Color.LightGray, parseColor("#12345"))
        assertEquals(Color.LightGray, parseColor("1234"))
    }

    @Test
    fun `parseColor with invalid hex length (too long for RRGGBB, not AARRGGBB) returns LightGray`() {
        assertEquals(Color.LightGray, parseColor("#1234567"))
        assertEquals(Color.LightGray, parseColor("1234567"))
    }

    @Test
    fun `parseColor with invalid hex length (too long for AARRGGBB) returns LightGray`() {
        assertEquals(Color.LightGray, parseColor("#123456789"))
        assertEquals(Color.LightGray, parseColor("123456789"))
    }

    @Test
    fun `parseColor with empty string returns LightGray`() {
        assertEquals(Color.LightGray, parseColor(""))
    }

    @Test
    fun `parseColor with only hash returns LightGray`() {
        assertEquals(Color.LightGray, parseColor("#"))
    }

    @Test
    fun `parseColor with non-hex string returns LightGray`() {
        assertEquals(Color.LightGray, parseColor("this is not a color"))
    }

    @Test
    fun `parseColor default color for known invalid case`() {
        val invalidColorString = "invalid"
        assertEquals(Color.LightGray, parseColor(invalidColorString))
    }
} 