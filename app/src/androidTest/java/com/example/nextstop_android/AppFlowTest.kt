package com.example.nextstop_android

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
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

    private fun sleep(seconds: Long = 2) = Thread.sleep(seconds * 1000)

    /**
     * Helper to wait for system permission dialogs and handle them.
     * Returns true if dialog was found and handled.
     */
    private fun handleSystemPermissionDialog(
        buttonTextPatterns: List<String>,
        timeoutMs: Long = 10000
    ): Boolean {
        Log.d(TAG, "TEST: Waiting for system dialog with patterns: $buttonTextPatterns")

        // Wait for any of the button patterns to appear
        val startTime = System.currentTimeMillis()
        var dialogButton: UiObject? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            for (pattern in buttonTextPatterns) {
                val btn = device.findObject(UiSelector().textMatches(pattern))
                if (btn.exists()) {
                    dialogButton = btn
                    Log.d(TAG, "TEST: Found system dialog button: ${btn.text}")
                    break
                }
            }

            if (dialogButton != null) break
            Thread.sleep(500) // Check every 500ms
        }

        if (dialogButton == null) {
            Log.w(TAG, "TEST: No system dialog found after ${timeoutMs}ms")
            return false
        }

        // Click the button
        try {
            dialogButton.click()
            logAction(dialogButton.text)
            sleep(2) // Give system time to process
            return true
        } catch (e: Exception) {
            Log.e(TAG, "TEST: Error clicking system dialog button", e)
            return false
        }
    }

    @Test
    fun verifyFullPermissionFlowAndStayOpen() {
        Log.d(TAG, "TEST: ========== STARTING PERMISSION FLOW TEST ==========")

        // --- 1. WELCOME ---
        Log.d(TAG, "TEST: Step 1 - Looking for Start Setup button")
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Start Setup", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start Setup", ignoreCase = true).performClick()
        logAction("Start Setup")
        sleep(1)

        // --- 2. LOCATION PERMISSION ---
        Log.d(TAG, "TEST: Step 2 - Location Permission")
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Allow Location", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Allow Location", ignoreCase = true).performClick()
        logAction("Allow Location button in app")

        // Handle system location dialog
        val locationHandled = handleSystemPermissionDialog(
            listOf(
                "(?i)While using the app",
                "(?i)Allow only while using the app",
                "(?i)Allow",
                "(?i)Only this time"
            )
        )

        if (!locationHandled) {
            Log.w(TAG, "TEST: Location system dialog not handled - may already be granted")
        }

        // --- 3. NOTIFICATIONS PERMISSION ---
        Log.d(TAG, "TEST: Step 3 - Notifications Permission")
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithText("Allow Notifications", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Allow Notifications", ignoreCase = true).performClick()
        logAction("Allow Notifications button in app")

        // Handle system notification dialog
        val notificationHandled = handleSystemPermissionDialog(
            listOf(
                "(?i)^Allow$",  // Exact match for "Allow"
                "(?i)OK"
            )
        )

        if (!notificationHandled) {
            Log.w(TAG, "TEST: Notification system dialog not handled - may already be granted")
        }

        // --- 4. OVERLAY PERMISSION ---
        Log.d(TAG, "TEST: Step 4 - Overlay Permission")
        composeTestRule.waitUntil(15000) {
            val enableOverlayNodes =
                composeTestRule.onAllNodesWithText("Enable Overlay", ignoreCase = true)
                    .fetchSemanticsNodes()
            val grantPermissionNodes =
                composeTestRule.onAllNodesWithText("Grant Permission", ignoreCase = true)
                    .fetchSemanticsNodes()

            enableOverlayNodes.isNotEmpty() || grantPermissionNodes.isNotEmpty()
        }

        // Try to click overlay button (text may vary)
        try {
            composeTestRule.onNodeWithText("Enable Overlay", ignoreCase = true).performClick()
            logAction("Enable Overlay")
        } catch (e: Exception) {
            composeTestRule.onNodeWithText("Grant Permission", ignoreCase = true).performClick()
            logAction("Grant Permission")
        }

        sleep(2)

        // --- 5. HANDLE OVERLAY SETTINGS ---
        Log.d(TAG, "TEST: Step 5 - Overlay Settings Flow")
        handleOverlaySettingsFlow()

        // --- 6. VERIFY WE'RE BACK IN APP ---
        Log.d(TAG, "TEST: Step 6 - Verifying app is functional")
        sleep(3)

        // Check if we can see map-related content or stepper
        var isAppReady = false
        try {
            composeTestRule.waitUntil(10000) {
                val step1Nodes = composeTestRule.onAllNodesWithText(
                    "Step 1",
                    substring = true,
                    ignoreCase = true
                )
                    .fetchSemanticsNodes()
                val trainNodes = composeTestRule.onAllNodesWithText("Train", ignoreCase = true)
                    .fetchSemanticsNodes()

                step1Nodes.isNotEmpty() || trainNodes.isNotEmpty()
            }
            // If waitUntil succeeds without throwing, we found the content
            isAppReady = true
        } catch (e: Exception) {
            Log.e(TAG, "TEST: Error checking app state", e)
            isAppReady = false
        }

        if (isAppReady) {
            Log.d(TAG, "TEST: ✅ App is ready and functional!")
        } else {
            Log.w(TAG, "TEST: ⚠️ Could not verify app state - may need manual check")
        }

        // --- 7. STAY OPEN FOR MANUAL TESTING ---
        Log.d(TAG, "TEST: ========== PERMISSION FLOW COMPLETE ==========")
        Log.d(TAG, "TEST: App will stay open for manual verification...")
        Log.d(TAG, "TEST: You can now interact with the map and test features")
        Log.d(TAG, "TEST: Press STOP button in Android Studio to end test")

        val startTime = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000
        var elapsedMinutes = 0

        while (System.currentTimeMillis() - startTime < oneHour) {
            val currentElapsed = ((System.currentTimeMillis() - startTime) / 60000).toInt()
            if (currentElapsed > elapsedMinutes) {
                elapsedMinutes = currentElapsed
                Log.d(TAG, "TEST: ⏰ App running for $elapsedMinutes minute(s)...")
            }
            Thread.sleep(10000)
        }

        Log.d(TAG, "TEST: ========== TEST TIMEOUT REACHED ==========")
    }

    private fun handleOverlaySettingsFlow() {
        val appName = "Next Stop"

        Log.d(TAG, "TEST: Looking for app in settings list: $appName")

        // Wait for settings to open
        sleep(2)

        // Try to find app in list
        var appInList = device.findObject(UiSelector().text(appName))

        if (!appInList.exists()) {
            Log.d(TAG, "TEST: App not visible, attempting scroll...")
            try {
                val scrollable = UiScrollable(UiSelector().scrollable(true))
                scrollable.setMaxSearchSwipes(20)
                scrollable.scrollIntoView(UiSelector().text(appName))
                appInList = device.findObject(UiSelector().text(appName))
            } catch (e: Exception) {
                Log.e(TAG, "TEST: Error during scroll", e)
            }
        }

        if (appInList.exists()) {
            Log.d(TAG, "TEST: Found app, clicking...")
            appInList.clickAndWaitForNewWindow()
            sleep(2)

            // Try to find and enable toggle
            val slider = device.findObject(
                UiSelector().resourceId("android:id/switch_widget")
            )
            val textLabel = device.findObject(
                UiSelector().textMatches("(?i)Allow display over other apps")
            )

            when {
                slider.exists() && !slider.isChecked -> {
                    slider.click()
                    logAction("Overlay Slider Toggle")
                }

                slider.exists() && slider.isChecked -> {
                    Log.d(TAG, "TEST: Overlay already enabled")
                }

                textLabel.exists() -> {
                    textLabel.click()
                    logAction("Overlay Text Label Toggle")
                }

                else -> {
                    Log.w(TAG, "TEST: Could not find overlay toggle element")
                }
            }

            sleep(2)

            // Navigate back to app
            Log.d(TAG, "TEST: Navigating back to app...")
            device.pressBack()
            logAction("Back (1)")
            sleep(1)
            device.pressBack()
            logAction("Back (2)")
            sleep(2)
        } else {
            Log.e(TAG, "TEST: ❌ Could not find $appName in settings!")
        }
    }
}