package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.UserGroupRelationshipException
import uk.gov.justice.digital.hmpps.externalusersapi.service.AdminType.EXT_ADM
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserRoleService.UserRoleException
import java.util.UUID

internal class UserRoleServiceTest {
  private val userRepository: UserRepository = mock()
  private val userRoleRepository: UserRoleRepository = mock()
  private val roleRepository: RoleRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val authentication: Authentication = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val service = UserRoleService(userRepository, maintainUserCheck, userRoleRepository, roleRepository, authenticationFacade, telemetryClient)

  @Nested
  inner class GetAuthUserByUserId {
    @Test
    fun getRolesSuccess(): Unit = runBlocking {
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      val id = UUID.randomUUID()
      val user = createSampleUser(username = "user")

      whenever(userRepository.findById(id)).thenReturn(user)

      whenever(roleRepository.findRolesByUserId(any())).thenReturn(
        flowOf(
          Authority(UUID.randomUUID(), "ROLE_ONE", "Role One info", adminType = "EXT_ADM"),
          Authority(UUID.randomUUID(), "GLOBAL_SEARCH", "Global Search", "Allow user to search globally for a user", "EXT_ADM"),
        ),
      )

      val roles = service.getUserRoles(id)
      assertThat(roles).extracting<String> { it.roleCode }.containsOnly("ROLE_ONE", "GLOBAL_SEARCH")
    }

    @Test
    fun getRolesUserNotFound(): Unit = runBlocking {
      val id = UUID.randomUUID()
      whenever(userRepository.findById(id)).thenReturn(null)
      val roles = service.getUserRoles(id)
      assertThat(roles).isNull()
    }
  }

  @Nested
  inner class AddRolesByUserId {
    @Test
    fun addRoles_userNotfound(): Unit = runBlocking {
      whenever(userRepository.findById(anyOrNull())).thenReturn(null)

      assertThatThrownBy {
        runBlocking {
          service.addRolesByUserId(
            UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
            listOf("ROLE_CODE"),
          )
        }
      }.isInstanceOf(
        UsernameNotFoundException::class.java,
      ).hasMessage("User 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a not found")
    }

    @Test
    fun addRoles_roleNotfound(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      whenever(userRepository.findById(anyOrNull())).thenReturn(createSampleUser(id = userId, username = "user"))
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf())

      assertThatThrownBy {
        runBlocking {
          service.addRolesByUserId(
            userId,
            listOf("        "),
          )
        }
      }.isInstanceOf(
        UserRoleException::class.java,
      ).hasMessage("Modify role failed for field role with reason: role.notfound")
    }

    @Test
    fun addRoles_invalidRole(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      whenever(userRepository.findById(any())).thenReturn(createSampleUser(id = UUID.randomUUID(), username = "user"))
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf())
      val role = Authority(UUID.randomUUID(), "FRED", "Role Fred", adminType = "EXT_ADM")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(role.copy(roleCode = "INCORRECT_ROLE")))

      assertThatThrownBy {
        runBlocking {
          service.addRolesByUserId(
            UUID.randomUUID(),
            listOf("BOB"),
          )
        }
      }.isInstanceOf(
        UserRoleException::class.java,
      ).hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun addRoles_noaccess(): Unit = runBlocking {
      whenever(userRepository.findById(any())).thenReturn(createSampleUser(id = UUID.randomUUID(), username = "user"))
      doThrow(UserGroupRelationshipException("user", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(anyString())

      assertThatThrownBy {
        runBlocking {
          service.addRolesByUserId(UUID.randomUUID(), listOf("BOB"))
        }
      }.isInstanceOf(
        UserGroupRelationshipException::class.java,
      ).hasMessage("Unable to maintain user: user with reason: User not with your groups")
    }

    @Test
    fun addRoles_invalidRoleGroupManager(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      whenever(userRepository.findById(any())).thenReturn(createSampleUser(id = UUID.randomUUID(), username = "user"))
      val role = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findByGroupAssignableRolesForUserId(any())).thenReturn(
        flowOf(
          Authority(
            UUID.randomUUID(),
            "FRED",
            "Role Fred",
            adminType = "EXT_ADM",
          ),
        ),
      )
      whenever(roleRepository.findRolesByUserId(any()))
        .thenReturn(flowOf(Authority(UUID.randomUUID(), "JOE", "bloggs", adminType = "EXT_ADM")))

      assertThatThrownBy {
        runBlocking {
          service.addRolesByUserId(
            UUID.randomUUID(),
            listOf("BOB"),
          )
        }
      }.isInstanceOf(
        UserRoleException::class.java,
      ).hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun addRoles_oauthAdminRestricted(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)

      whenever(userRepository.findById(any())).thenReturn(createSampleUser(id = UUID.randomUUID(), username = "user"))
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf())
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf()) // allroles
      val role = Authority(UUID.randomUUID(), "ROLE_OAUTH_ADMIN", "Role Licence Vary", adminType = "EXT_ADM")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)

      assertThatThrownBy {
        runBlocking { service.addRolesByUserId(UUID.randomUUID(), listOf("BOB")) }
      }.isInstanceOf(
        UserRoleException::class.java,
      ).hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun addRoles_roleAlreadyOnUser(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)

      val userId = UUID.randomUUID()
      whenever(userRepository.findById(userId)).thenReturn(createSampleUser(id = UUID.randomUUID(), username = "user"))
      val role = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findRolesByUserId(userId)).thenReturn(flowOf(role))
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(role)) // allroles

      assertThatThrownBy {
        runBlocking {
          service.addRolesByUserId(
            userId,
            listOf("LICENCE_VARY"),
          )
        }
      }.isInstanceOf(
        UserRoleException::class.java,
      ).hasMessage("Modify role failed for field role with reason: role.exists")
    }

    @Test
    fun addRoles_oauthAdminRestricted_success(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"), SimpleGrantedAuthority("ROLE_OAUTH_ADMIN")))

      val userId = UUID.randomUUID()
      whenever(userRepository.findById(userId)).thenReturn(createSampleUser(id = userId, username = "user"))
      val role = Authority(UUID.randomUUID(), "ROLE_OAUTH_ADMIN", "Role Auth Admin", adminType = "EXT_ADM")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(role)) // allroles
      whenever(roleRepository.findRolesByUserId(userId)).thenReturn(flowOf(role.copy(roleCode = "ANY")))

      service.addRolesByUserId(userId, listOf("TO_ADD"))
      verify(telemetryClient).trackEvent(
        "ExternalUserRoleAddSuccess",
        mapOf("userId" to userId.toString(), "username" to "user", "role" to "TO_ADD", "admin" to "admin"),
        null,
      )
    }

    @Test
    fun addRoles_success(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)

      val userId = UUID.randomUUID()
      whenever(userRepository.findById(userId)).thenReturn(createSampleUser(id = userId, username = "user"))
      val role = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(role)) // allroles
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findRolesByUserId(userId)).thenReturn(flowOf(role.copy(roleCode = "ANY")))

      service.addRolesByUserId(userId, listOf("ROLE_LICENCE_VARY"))
      verify(telemetryClient).trackEvent(
        "ExternalUserRoleAddSuccess",
        mapOf("userId" to userId.toString(), "username" to "user", "role" to "LICENCE_VARY", "admin" to "admin"),
        null,
      )
    }

    @Test
    fun `addRoles success multiple roles`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)

      val userId = UUID.randomUUID()
      whenever(userRepository.findById(any())).thenReturn(createSampleUser(id = userId, username = "user"))
      val role1 = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      val role2 = Authority(UUID.randomUUID(), "ROLE_OTHER", "Role Other", adminType = "EXT_ADM")
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(role1, role2)) // allroles
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role1).thenReturn(role2)
      whenever(roleRepository.findRolesByUserId(userId))
        .thenReturn(flowOf(Authority(UUID.randomUUID(), "JOE", "bloggs", adminType = "EXT_ADM")))

      service.addRolesByUserId(userId, listOf("ROLE_LICENCE_VARY", "ROLE_OTHER"))
      verify(telemetryClient).trackEvent(
        "ExternalUserRoleAddSuccess",
        mapOf("userId" to userId.toString(), "username" to "user", "role" to "LICENCE_VARY", "admin" to "admin"),
        null,
      )
      verify(telemetryClient).trackEvent(
        "ExternalUserRoleAddSuccess",
        mapOf("userId" to userId.toString(), "username" to "user", "role" to "OTHER", "admin" to "admin"),
        null,
      )
    }

    @Test
    fun addRoles_successGroupManager(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val userId = UUID.randomUUID()
      whenever(userRepository.findById(any())).thenReturn(createSampleUser(id = userId, username = "user"))
      val role = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(role)) // allroles
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findByGroupAssignableRolesForUserId(any())).thenReturn(flowOf(role))
      whenever(roleRepository.findRolesByUserId(userId))
        .thenReturn(flowOf(Authority(UUID.randomUUID(), "JOE", "bloggs", adminType = "EXT_ADM")))

      service.addRolesByUserId(
        userId,
        listOf("ROLE_LICENCE_VARY"),
      )
      verify(telemetryClient).trackEvent(
        "ExternalUserRoleAddSuccess",
        mapOf("userId" to userId.toString(), "username" to "user", "role" to "LICENCE_VARY", "admin" to "admin"),
        null,
      )
    }
  }

  @Nested
  inner class RemoveRoleByUserId {

    @Test
    fun removeRole_userNotFound(): Unit = runBlocking {
      whenever(userRepository.findById(any())).thenReturn(null)

      val roles = service.getUserRoles(UUID.randomUUID())
      assertThat(roles).isNull()
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun removeRole_roleNotOnUser(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(any())).thenReturn(user)
      val role = Authority(UUID.randomUUID(), "BOB", "Bloggs", adminType = "EXT_ADM")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(role.copy(roleCode = "INCORRECT_ROLE")))

      assertThatThrownBy {
        runBlocking {
          service.removeRoleByUserId(
            UUID.randomUUID(),
            "BOB",
          )
        }
      }.isInstanceOf(UserRoleException::class.java)
        .hasMessage("Modify role failed for field role with reason: role.missing")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun removeRole_invalid(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)

      val role = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      val role2 = Authority(UUID.randomUUID(), "BOB", "Bloggs", adminType = "EXT_ADM")
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(any())).thenReturn(user)
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role2)
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(role))
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(role2))

      assertThatThrownBy {
        runBlocking {
          service.removeRoleByUserId(UUID.randomUUID(), "BOB")
        }
      }.isInstanceOf(UserRoleException::class.java)
        .hasMessage("Modify role failed for field role with reason: invalid")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun removeRole_noaccess(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      doThrow(UserGroupRelationshipException("user", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(anyString())
      whenever(userRepository.findById(any())).thenReturn(user)

      assertThatThrownBy {
        runBlocking {
          service.removeRoleByUserId(UUID.randomUUID(), "R")
        }
      }.isInstanceOf(
        UserGroupRelationshipException::class.java,
      ).hasMessage("Unable to maintain user: user with reason: User not with your groups")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun removeRole_invalidGroupManager(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)

      val role = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      val role2 = Authority(UUID.randomUUID(), "BOB", "Bloggs", adminType = "EXT_ADM")
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(any())).thenReturn(user)
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(role2))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role2)
      whenever(roleRepository.findByGroupAssignableRolesForUserId(any())).thenReturn(flowOf(role))

      assertThatThrownBy {
        runBlocking {
          service.removeRoleByUserId(UUID.randomUUID(), "BOB")
        }
      }.isInstanceOf(UserRoleException::class.java).hasMessage("Modify role failed for field role with reason: invalid")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun removeRole_notfound(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(any())).thenReturn(user)

      assertThatThrownBy {
        runBlocking {
          service.removeRoleByUserId(UUID.randomUUID(), "BOB")
        }
      }.isInstanceOf(UserRoleException::class.java)
        .hasMessage("Modify role failed for field role with reason: role.notfound")
    }

    @Test
    fun removeRole_success(): Unit = runBlocking {
      val userId = UUID.randomUUID()
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(any())).thenReturn(user)
      val roleToRemoveId = UUID.randomUUID()
      val role = Authority(UUID.randomUUID(), "LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      val role2 = Authority(roleToRemoveId, "JOE", "Bloggs", adminType = "EXT_ADM")
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(role, role2))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode))
        .thenReturn(flowOf(role, role2))

      service.removeRoleByUserId(userId, "  licence_vary   ")
      verify(telemetryClient).trackEvent(
        "ExternalUserRoleRemoveSuccess",
        mapOf("userId" to userId.toString(), "username" to "user", "role" to "LICENCE_VARY", "admin" to "admin"),
        null,
      )
    }

    @Test
    fun removeRole_successGroupManager(): Unit = runBlocking {
      val userId = UUID.randomUUID()
      whenever(authenticationFacade.getUsername()).thenReturn("groupmanager")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val role = Authority(UUID.randomUUID(), "LICENCE_VARY", "Role Licence Vary", adminType = "EXT_ADM")
      val role2 = Authority(UUID.randomUUID(), "JOE", "Bloggs", adminType = "EXT_ADM")
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(any())).thenReturn(user)
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(role)
      whenever(roleRepository.findByGroupAssignableRolesForUserId(any())).thenReturn(flowOf(role, role2))
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(role))

      service.removeRoleByUserId(userId, "  licence_vary   ")
      verify(telemetryClient).trackEvent(
        "ExternalUserRoleRemoveSuccess",
        mapOf("userId" to userId.toString(), "username" to "user", "role" to "LICENCE_VARY", "admin" to "groupmanager"),
        null,
      )
    }
  }

  @Nested
  inner class AssignableRolesByUserId {
    @Test
    fun `assignable roles for group manager`(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)
      val first = Authority(UUID.randomUUID(), "FIRST", "Role First", adminType = "EXT_ADM")
      val second = Authority(UUID.randomUUID(), "SECOND", "Role Second", adminType = "EXT_ADM")
      val fred = Authority(UUID.randomUUID(), "FRED", "Role Fred", adminType = "EXT_ADM")
      val joe = Authority(UUID.randomUUID(), "JOE", "Role Joe", adminType = "EXT_ADM")

      whenever(roleRepository.findByGroupAssignableRolesForUserId(any())).thenReturn(flowOf(first, fred, second))
      whenever(userRepository.findById(any())).thenReturn(createSampleUser())
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(fred, joe))

      assertThat(
        service.getAssignableRolesByUserId(UUID.randomUUID()),
      ).containsExactly(first, second)
    }

    @Test
    fun `assignable roles for super user`(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      val first = Authority(UUID.randomUUID(), "FIRST", "Role First", adminType = "EXT_ADM")
      val second = Authority(UUID.randomUUID(), "SECOND", "Role Second", adminType = "EXT_ADM")
      val fred = Authority(UUID.randomUUID(), "FRED", "Role Fred", adminType = "EXT_ADM")
      val joe = Authority(UUID.randomUUID(), "JOE", "Role Joe", adminType = "EXT_ADM")

      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(first, fred, second))
      whenever(userRepository.findById(any())).thenReturn(createSampleUser())
      whenever(roleRepository.findRolesByUserId(any())).thenReturn(flowOf(fred, joe))

      assertThat(service.getAssignableRolesByUserId(UUID.randomUUID())).containsExactly(first, second)
    }
  }

  @Nested
  inner class AllAssignableRolesByUserId {
    @Test
    fun `all assignable roles for group manager`(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)
      val first = Authority(UUID.randomUUID(), "FIRST", "Role First", adminType = "EXT_ADM")
      val second = Authority(UUID.randomUUID(), "SECOND", "Role Second", adminType = "EXT_ADM")
      val fred = Authority(UUID.randomUUID(), "FRED", "Role Fred", adminType = "EXT_ADM")
      whenever(roleRepository.findByGroupAssignableRolesForUserId(any())).thenReturn(flowOf(first, fred, second))

      assertThat(service.getAllAssignableRolesByUserId(UUID.randomUUID())).containsOnly(first, fred, second)
      verify(roleRepository, never()).findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)
    }

    @Test
    fun `all assignable roles for Super User`(): Unit = runBlocking {
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      val first = Authority(UUID.randomUUID(), "FIRST", "Role First", adminType = "EXT_ADM")
      val second = Authority(UUID.randomUUID(), "SECOND", "Role Second", adminType = "EXT_ADM")
      val fred = Authority(UUID.randomUUID(), "FRED", "Role Fred", adminType = "EXT_ADM")
      val joe = Authority(UUID.randomUUID(), "JOE", "Role Joe", adminType = "EXT_ADM")
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(first, fred, second, joe))

      assertThat(service.getAllAssignableRolesByUserId(UUID.randomUUID())).containsOnly(first, fred, second, joe)
      verify(roleRepository, never()).findByGroupAssignableRolesForUserId(any())
    }
  }

  @Nested
  inner class AllAssignableRoles {
    @Test
    fun `all assignable roles for group manager`(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      val first = Authority(UUID.randomUUID(), "FIRST", "Role First", adminType = "EXT_ADM")
      val second = Authority(UUID.randomUUID(), "SECOND", "Role Second", adminType = "EXT_ADM")
      val fred = Authority(UUID.randomUUID(), "FRED", "Role Fred", adminType = "EXT_ADM")

      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(first, second, fred))

      assertThat(service.getAllAssignableRoles()).containsOnly(first, fred, second)
      verify(roleRepository, never()).findByGroupAssignableRolesForUserName(EXT_ADM.adminTypeCode)
    }

    @Test
    fun `all assignable roles for Super User`(): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER_ROLE)
      val first = Authority(UUID.randomUUID(), "FIRST", "Role First", adminType = "EXT_ADM")
      val second = Authority(UUID.randomUUID(), "SECOND", "Role Second", adminType = "EXT_ADM")
      val fred = Authority(UUID.randomUUID(), "FRED", "Role Fred", adminType = "EXT_ADM")
      val joe = Authority(UUID.randomUUID(), "JOE", "Role Joe", adminType = "EXT_ADM")
      whenever(roleRepository.findByAdminTypeContainingOrderByRoleName(EXT_ADM.adminTypeCode)).thenReturn(flowOf(first, second, fred, joe))

      assertThat(service.getAllAssignableRoles()).containsOnly(first, fred, second, joe)
      verify(roleRepository, never()).findByGroupAssignableRolesForUserName(anyString())
    }

    @Nested
    inner class ListOfRolesForUser {
      @Test
      fun userRoles_notFound(): Unit = runBlocking {
        whenever(userRepository.findByUsernameAndSource(anyOrNull(), anyOrNull())).thenReturn(null)
        assertThatThrownBy { runBlocking { service.getRolesByUsername("JOE") } }
          .isInstanceOf(UsernameNotFoundException::class.java)
          .hasMessage("User with username JOE not found")
      }

      @Test
      fun userRoles_externalUser(): Unit = runBlocking {
        val mockUser = User("JOE", AuthSource.auth)
        whenever(userRepository.findByUsernameAndSource(anyOrNull(), anyOrNull())).thenReturn(mockUser)
        val role1 = Authority(UUID.randomUUID(), "FRED", "FRED", adminType = "EXT_ADM")
        val role2 = Authority(UUID.randomUUID(), "GLOBAL_SEARCH", "Global Search", "Allow user to search globally for a user", adminType = "EXT_ADM")
        whenever(roleRepository.findByUserRolesForUserName(any())).thenReturn(flowOf(role1, role2))
        assertThat(service.getRolesByUsername("JOE")).containsOnly(role1, role2)
        verify(roleRepository).findByUserRolesForUserName("JOE")
      }
    }
  }

  companion object {
    private val SUPER_USER_ROLE: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER_ROLE: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
