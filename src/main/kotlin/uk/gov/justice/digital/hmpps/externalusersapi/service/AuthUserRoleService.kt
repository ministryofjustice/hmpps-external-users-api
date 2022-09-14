package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority

@Service

@Transactional(readOnly = true)
class AuthUserRoleService(
  private val roleRepository: RoleRepository,
) {
  val allRoles: List<Authority>
    get() = roleRepository.findAllByOrderByRoleNameLike(AdminType.EXT_ADM.adminTypeCode)
}
