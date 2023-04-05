package uk.gov.justice.digital.hmpps.externalusersapi.repository

import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.UserRole

@Repository
interface UserRoleCoroutineRepository : CoroutineSortingRepository<UserRole, String>
