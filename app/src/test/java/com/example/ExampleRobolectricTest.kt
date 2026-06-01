package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.domain.usecase.ExportXlsxUseCase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EditFlow Pro", appName)
  }

  @Test
  fun `test Excel export usecase execution`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val useCase = ExportXlsxUseCase()
    val result = useCase.execute(context, "test_report", emptyList())
    if (result.isSuccess) {
        println("=== EXPORT SUCCESS ===")
    } else {
        println("=== EXPORT FAILURE DETAILS ===")
        val exception = result.exceptionOrNull()
        exception?.printStackTrace()
        throw exception ?: RuntimeException("Unknown export error")
    }
  }
}
