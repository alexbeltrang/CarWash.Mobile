package com.carwash.mobile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carwash.mobile.ingreso.PlateReviewContent
import com.carwash.mobile.ingreso.PlateTextReader
import com.carwash.mobile.ingreso.plateCameraTouchListener
import com.carwash.mobile.ui.theme.CarWashTheme
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlateCaptureTest {
    @get:Rule
    val composeRule = createComposeRule()
    private var confirmed: String? = null
    private var retried = false
    private val zoomFactors = mutableListOf<Float>()
    private val focusPoints = mutableListOf<Pair<Float, Float>>()
    private var previewClicks = 0

    @Test
    fun twoFingerZoomInAndOutDoesNotTriggerFocusOrClick() {
        touchPreview()
        pinchPreview(zoomIn = true)
        composeRule.runOnIdle {
            assertTrue("No se detecto acercamiento", zoomFactors.any { it > 1f })
            zoomFactors.clear()
        }
        pinchPreview(zoomIn = false)
        composeRule.runOnIdle {
            assertTrue("No se detecto alejamiento", zoomFactors.any { it < 1f })
            assertTrue(focusPoints.isEmpty())
            assertEquals(0, previewClicks)
        }
    }

    @Test
    fun singleTapStillFocusesAfterZoomGesture() {
        touchPreview()
        pinchPreview(zoomIn = true)
        composeRule.onNodeWithTag("touchPreview").performTouchInput { click(center) }
        composeRule.runOnIdle {
            assertEquals(1, focusPoints.size)
            assertTrue(focusPoints.single().first > 0f)
            assertTrue(focusPoints.single().second > 0f)
            assertEquals(1, previewClicks)
        }
    }

    @Test
    fun reviewRequiresConfirmationAndPreservesCorrectedCharacters() {
        review(listOf("ABC123"))
        composeRule.runOnIdle { assertNull(confirmed) }
        composeRule.onNodeWithTag("reviewPlate").performTextReplacement(" aoi012 ")
        composeRule.onNodeWithTag("confirmPlate").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals("AOI012", confirmed) }
    }

    @Test
    fun emptyReadingAllowsManualPlateWithoutNewFormatRestriction() {
        review(emptyList())
        composeRule.onNodeWithText("No se detectó una placa").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmPlate").assertIsNotEnabled()
        composeRule.onNodeWithTag("reviewPlate").performTextReplacement("especial")
        composeRule.onNodeWithTag("confirmPlate").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals("ESPECIAL", confirmed) }
    }

    @Test
    fun choosingAlternativeAndRetryDoNotApplyAPlate() {
        review(listOf("ABC123", "XYZ45D"))
        composeRule.onNodeWithText("XYZ45D").performClick()
        composeRule.onNodeWithTag("retryCapture").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(retried); assertNull(confirmed) }
    }

    @Test
    fun reviewActionsRemainReachableWithLargeFont() {
        review(listOf("ABC123", "XYZ45D", "AOI012"), fontScale = 1.8f)
        composeRule.onNodeWithTag("reviewPlate").performScrollTo().performTextReplacement("XYZ789")
        composeRule.onNodeWithTag("confirmPlate").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("XYZ789", confirmed) }
    }

    @Test
    fun bundledRecognizerReadsSyntheticPlate() {
        val bitmap = plateBitmap()
        try {
            PlateTextReader().use { reader ->
                val candidates = Tasks.await(reader.read(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
                assertTrue("No reconocio la placa sintetica", "ABC123" in candidates)
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun bundledRecognizerReturnsEmptyOnBlankImage() {
        val bitmap = Bitmap.createBitmap(800, 240, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        try {
            PlateTextReader().use { reader ->
                assertTrue(Tasks.await(reader.read(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS).isEmpty())
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun bundledRecognizerRespectsImageRotation() {
        val source = plateBitmap()
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            Matrix().apply { postRotate(180f) }, true)
        try {
            PlateTextReader().use { reader ->
                val candidates = Tasks.await(reader.read(InputImage.fromBitmap(rotated, 180)), 30, TimeUnit.SECONDS)
                assertTrue("No reconocio la placa rotada", "ABC123" in candidates)
            }
        } finally {
            source.recycle()
            rotated.recycle()
        }
    }

    private fun touchPreview() {
        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    View(context).apply {
                        setOnClickListener { previewClicks++ }
                        setOnTouchListener(plateCameraTouchListener(
                            context,
                            onZoom = { zoomFactors += it },
                            onFocus = { horizontal, vertical -> focusPoints += horizontal to vertical },
                        ))
                    }
                },
                modifier = Modifier.fillMaxSize().testTag("touchPreview"),
            )
        }
    }

    private fun pinchPreview(zoomIn: Boolean) {
        composeRule.onNodeWithTag("touchPreview").performTouchInput {
            val near = width * .12f
            val far = width * .38f
            val start = if (zoomIn) near else far
            val end = if (zoomIn) far else near
            pinch(
                start0 = center + Offset(-start, 0f),
                end0 = center + Offset(-end, 0f),
                start1 = center + Offset(start, 0f),
                end1 = center + Offset(end, 0f),
            )
        }
    }

    private fun review(candidates: List<String>, fontScale: Float = 1f) {
        composeRule.setContent {
            var value by remember { mutableStateOf(candidates.firstOrNull().orEmpty()) }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                CarWashTheme {
                    PlateReviewContent(candidates, value, { value = it }, { confirmed = it }, { retried = true })
                }
            }
        }
    }

    private fun plateBitmap(): Bitmap = Bitmap.createBitmap(800, 240, Bitmap.Config.ARGB_8888).also { bitmap ->
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawText("ABC123", 48f, 170f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 140f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
        }
    }
}