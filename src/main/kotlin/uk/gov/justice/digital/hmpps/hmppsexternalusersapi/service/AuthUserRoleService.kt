package uk.gov.justice.digital.hmpps.hmppsexternalusersapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.model.Authority

@Service

@Transactional(readOnly = true)
class AuthUserRoleService(
  private val roleRepository: RoleRepository,
) {
  val allRoles: List<Authority>
    get() = roleRepository.findAllByOrderByRoleNameLike(AdminType.EXT_ADM.adminTypeCode)
}
