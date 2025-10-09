package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class CRSGroupCheckTest {

  private val crsGroupCheck = CRSGroupCheck()

  @Test
  fun `is crs group code`() {
    assertTrue(crsGroupCheck.isCRSGroupCode("INT_CR_GROUP_1"))
    assertTrue(crsGroupCheck.isCRSGroupCode("INT_SP_GROUP_1"))
  }

  @Test
  fun `is not crs group code`() {
    assertFalse(crsGroupCheck.isCRSGroupCode("INT_GROUP_1"))
  }
}
