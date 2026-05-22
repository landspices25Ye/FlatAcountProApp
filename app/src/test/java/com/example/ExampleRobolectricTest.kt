package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun read_string_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("المحاسب المالي", appName)
  }

  @Test
  fun test_all_tabs_click_to_ensure_no_crashes() {
    val tabs = listOf(
      "الرئيسية",
      "الحسابات (دليل)",
      "القيود اليومية",
      "دفتر الميزان",
      "الفواتير",
      "المخازن",
      "شؤون الموظفين",
      "التقارير المالية"
    )

    for (tabTitle in tabs) {
      composeTestRule.onNodeWithText(tabTitle).performClick()
      composeTestRule.waitForIdle()
    }
  }
}
