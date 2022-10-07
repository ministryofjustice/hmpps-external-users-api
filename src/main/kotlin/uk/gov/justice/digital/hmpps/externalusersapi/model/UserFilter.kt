package uk.gov.justice.digital.hmpps.externalusersapi.model

import com.google.common.collect.ImmutableList
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.util.EmailHelper.format
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class UserFilter(
  name: String? = null,
  val roleCodes: List<String>? = null,
  val groupCodes: List<String>? = null,
  val status: Status = Status.ALL,
  val authSources: List<AuthSource>? = null,
) : Specification<User> {

  private val name: String? = if (name.isNullOrBlank()) null else name.trim()

  override fun toPredicate(
    root: Root<User>,
    query: CriteriaQuery<*>,
    cb: CriteriaBuilder
  ): Predicate {
    val andBuilder = ImmutableList.builder<Predicate>()

    // Defaults to AuthSource.auth when no authSources provided
    andBuilder.add(buildSourcesPredicate(root, cb, authSources))

    if (!roleCodes.isNullOrEmpty()) {
      val rolePredicate =
        root.join<Any, Any>("authorities").get<Any>("roleCode").`in`(roleCodes.map { it.trim().uppercase() })
      andBuilder.add(rolePredicate)
    }
    if (!groupCodes.isNullOrEmpty()) {
      val groupPredicate =
        root.join<Any, Any>("groups").get<Any>("groupCode").`in`(groupCodes.map { it.trim().uppercase() })
      andBuilder.add(groupPredicate)
    }
    if (!name.isNullOrBlank()) {
      andBuilder.add(buildNamePredicate(root, cb))
    }
    if (status != Status.ALL) {
      andBuilder.add(cb.equal(root.get<Any>("enabled"), status == Status.ACTIVE))
    }
    query.distinct(true)
//    val personJoin = root.join<Any, Any>("person", JoinType.INNER)
    // query.orderBy(cb.asc(personJoin.get<Any>("firstName")), cb.asc(personJoin.get<Any>("lastName")))
    return cb.and(*andBuilder.build().toTypedArray())
  }

  private fun buildSourcesPredicate(root: Root<User>, cb: CriteriaBuilder, sources: List<AuthSource>?): Predicate {
    if (!sources.isNullOrEmpty()) {
      val orBuilder = ImmutableList.builder<Predicate>()
      sources.forEach {
        orBuilder.add(cb.equal(root.get<Any>("source"), it))
      }
      return cb.or(*orBuilder.build().toTypedArray())
    }
    return cb.equal(root.get<Any>("source"), AuthSource.auth)
  }

  private fun buildNamePredicate(root: Root<User>, cb: CriteriaBuilder): Predicate {
    val orBuilder = ImmutableList.builder<Predicate>()
    val pattern = "%" + name!!.replace(',', ' ').replace(" [ ]*".toRegex(), "% %") + "%"
    orBuilder.add(cb.like(root.get("email"), format(pattern)))
    orBuilder.add(cb.like(root.get("username"), pattern.uppercase()))
    val personJoin = root.join<Any, Any>("person", JoinType.INNER)
    orBuilder.add(
      cb.like(
        cb.lower(cb.concat(cb.concat(personJoin.get("firstName"), " "), personJoin.get("lastName"))),
        pattern.lowercase()
      )
    )
    orBuilder.add(
      cb.like(
        cb.lower(cb.concat(cb.concat(personJoin.get("lastName"), " "), personJoin.get("firstName"))),
        pattern.lowercase()
      )
    )
    return cb.or(*orBuilder.build().toTypedArray())
  }

  enum class Status {
    ACTIVE, INACTIVE, ALL
  }
}
