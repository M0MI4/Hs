package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HsStore", appName)
  }

  @Test
  fun `string list converter serializes and deserializes correctly`() {
    val converter = com.example.data.local.StringListConverter()
    val input = listOf("Rootless Hook", "No ADS", "Anti-Ban")
    val json = converter.fromStringList(input)
    val output = converter.toStringList(json)
    assertEquals(input, output)
  }

  @Test
  fun `install state returns GET for uninstalled package`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val state = com.example.util.InstallStatusHelper.getAppInstallState(context, "com.nonexistent.package.app", 1)
    assertEquals(com.example.util.AppInstallState.GET, state)
  }
}
