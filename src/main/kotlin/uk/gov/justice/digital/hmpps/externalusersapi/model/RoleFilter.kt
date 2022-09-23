package uk.gov.justice.digital.hmpps.externalusersapi.model

import com.google.common.collect.ImmutableList
import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import kotlin.reflect.KProperty1

class RoleFilter(
  roleName: String? = null,
  val roleCode: String? = null,
  val adminTypes: List<AdminType>? = null,
) : Specification<Authority> {

  private val roleName: String? = if (roleName.isNullOrBlank()) null else roleName.trim()

  fun <PROP> Root<*>.get(prop: KProperty1<*, PROP>): Path<PROP> = this.get(prop.name)

  override fun toPredicate(
    root: Root<Authority>,
    query: CriteriaQuery<*>,
    cb: CriteriaBuilder
  ): Predicate? {
    val andBuilder = ImmutableList.builder<Predicate>()

    if (!roleName.isNullOrBlank()) {
      andBuilder.add(buildRoleNamePredicate(root, cb))
    }

    if (!roleCode.isNullOrBlank()) {
      val pattern = "%" + roleCode.trim().replace(',', ' ').replace(" [ ]*".toRegex(), "% %") + "%"
      andBuilder.add(cb.like(root.get(Authority::roleCode), pattern.uppercase()))
    }

    // Defaults to all when no admin types provided
    if (!adminTypes.isNullOrEmpty()) {
      andBuilder.add(buildAdminTypesPredicate(root, cb, adminTypes))
    }

    query.distinct(true)
    return cb.and(*andBuilder.build().toTypedArray())
  }

  private fun buildRoleNamePredicate(root: Root<Authority>, cb: CriteriaBuilder): Predicate {
    val orBuilder = ImmutableList.builder<Predicate>()
    val pattern = "%" + roleName!!.replace(',', ' ').replace(" [ ]*".toRegex(), "% %") + "%"
    orBuilder.add(cb.like(cb.lower(root.get(Authority::roleName)), pattern.lowercase()))
    return cb.or(*orBuilder.build().toTypedArray())
  }

  private fun buildAdminTypesPredicate(root: Root<Authority>, cb: CriteriaBuilder, adminTypes: List<AdminType>): Predicate {
    val andBuilder = ImmutableList.builder<Predicate>()

    adminTypes.forEach {
      val pattern = "%" + it.adminTypeCode + "%"
      andBuilder.add(cb.like(root.get(Authority::adminTypesAsString), pattern))
    }

    return cb.and(*andBuilder.build().toTypedArray())
  }
}
