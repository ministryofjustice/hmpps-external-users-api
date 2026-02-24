package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.AutoConfigureDataR2dbc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureDataR2dbc
class GroupAssignableRoleRepositoryTest {
  @Autowired
  private lateinit var repository: GroupAssignableRoleRepository

  @Test
  fun `findGroupAssignableRoleByGroupCode returns roles for valid groupCode`() = runBlocking {
    val groupCode = "PECS_MPS34"
    val groupAssignableRole = repository.findGroupAssignableRoleByGroupCode(groupCode).toList()

    assertTrue(groupAssignableRole.isNotEmpty())
    assertEquals("PECS_PER_AUTHOR", groupAssignableRole[0].roleCode)
  }

  @Test
  fun `findGroupAssignableRoleByGroupCode returns empty for invalid groupCode`() = runBlocking {
    val groupCode = "INVALID_GROUP"
    val groupAssignableRole = repository.findGroupAssignableRoleByGroupCode(groupCode).toList()

    assertTrue(groupAssignableRole.isEmpty())
  }

  @Test
  fun `findGroupAssignableRoleByGroupCodeAndRoleCode returns role for valid groupCode and roleCode`() = runBlocking {
    val groupCode = "PECS_MPS34"
    val roleCode = "PECS_PER_AUTHOR"
    val groupAssignableRole = repository.findGroupAssignableRoleByGroupCodeAndRoleCode(groupCode, roleCode).toList()

    assertTrue(groupAssignableRole.isNotEmpty())
    assertEquals(UUID.fromString("e6a53633-7e45-4b42-b6a8-e95ab1039d8d"), groupAssignableRole[0].roleId)
  }

  @Test
  fun `findGroupAssignableRoleByGroupCodeAndRoleCode returns empty for invalid groupCode or roleCode`() = runBlocking {
    val groupCode = "GROUP1"
    val roleCode = "INVALID_ROLE"
    val groupAssignableRole = repository.findGroupAssignableRoleByGroupCodeAndRoleCode(groupCode, roleCode).toList()

    assertTrue(groupAssignableRole.isEmpty())
  }
}
