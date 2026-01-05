package com.example.nextstop_android

import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PermissionTest {

    private val TAG = "PermissionTest"

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        Log.d(TAG, "TEST: Environment initialized")
    }

    private fun logAction(btnName: String) {
        Log.d(TAG, "TEST: SUCCESS - Button [$btnName] pressed.")
    }

    private fun watch(seconds: Long = 2) = Thread.sleep(seconds * 1000)

    @Test
    fun verifyFullPermissionFlowAndStayOpen() {
        // --- 1. WELCOME ---
        composeTestRule.onNodeWithText("Start Setup", ignoreCase = true).performClick()
        logAction("Start Setup")

        // --- 2. LOCATION ---
        composeTestRule.waitUntil(8000) {
            composeTestRule.onAllNodesWithText("Enable Location", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Enable Location", ignoreCase = true).performClick()

        val whileUsingBtn = device.findObject(UiSelector().textMatches("(?i)While using the app|Allow only while using the app"))
        if (whileUsingBtn.waitForExists(8000)) {
            whileUsingBtn.click()
            logAction("While using the app")
            watch(3)
        }

        // --- 3. NOTIFICATIONS ---
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Enable Notifications", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Enable Notifications", ignoreCase = true).performClick()

        val allowNotifyBtn = device.findObject(UiSelector().text("Allow"))
        if (allowNotifyBtn.waitForExists(5000)) {
            allowNotifyBtn.click()
            logAction("Allow (Notifications)")
            watch(3)
        }

        // --- 4. OVERLAY / ALARM ---
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Grant Permission", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Grant Permission", ignoreCase = true).performClick()
        logAction("Grant Permission")

        // --- 5. SETTINGS FLOW ---
        handleOverlaySettingsFlow()

        // --- 6. THE "STAY OPEN" LOOP ---
        Log.d(TAG, "TEST: --- FLOW COMPLETE ---")
        Log.d(TAG, "TEST: App will now stay open for manual map verification.")

        // This loop keeps the test alive for 1 hour.
        // You can stop it manually by clicking the 'Stop' (Red square) button in Android Studio.
        val startTime = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000

        while (System.currentTimeMillis() - startTime < oneHour) {
            // We log every 10 seconds just to show the test is still alive
            Log.d(TAG, "TEST: Keeping app alive... check your Map now!")
            Thread.sleep(10000)
        }
    }

    private fun handleOverlaySettingsFlow() {
        val appName = "Next Stop"
        val appInList = device.findObject(UiSelector().text(appName))

        if (!appInList.waitForExists(10000)) {
            UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().text(appName))
        }

        if (appInList.exists()) {
            appInList.clickAndWaitForNewWindow()
            watch(3)

            val slider = device.findObject(UiSelector().resourceId("android:id/switch_widget"))
            val textLabel = device.findObject(UiSelector().textMatches("(?i)Allow display over other apps"))

            if (slider.exists() && !slider.isChecked) {
                slider.click()
                logAction("Slider Toggle")
            } else if (textLabel.exists()) {
                textLabel.click()
                logAction("Text Label Toggle")
            }

            watch(4)
            device.pressBack()
            logAction("Back Arrow (1)")
            watch(2)
            device.pressBack()
            logAction("Back Arrow (2)")
            watch(2)
        }
    }
}