package mongobeans

/*
 * This file contains code in support of making securing individual actions
 * on your mongobeans.  The basic approach is that in addition to having
 * typed attributes, a mongobean may also have many actions in the form
 * of methods.  However, instead of simply defining methods on the bean, 
 * this library offers a base class called ApiCall. 
 */

/**
 * The base trait for a user, which applications must mix in for this
 * security library to work.
 */
trait User {
  val id: String
}

/**
 * This trait defines the default security check, which is to always allow
 * access.  Sub-traits of this trait will define more interesting behaviors.
 */
trait GenericSecurityCheck {
  def isAllowed(u:User): Boolean = true
}

/**
 * This is the Exception that gets thrown when someone attempts to do something
 * that they do not have permission to do.
 */
class PermissionDenied extends RuntimeException

/**
 * The ApiCall base class is an abstract class from which new API calls 
 * may be derived.
 */
abstract class ApiCall[Parameters] extends GenericSecurityCheck {
  def apply(p: Parameters)(implicit u: User) = {
    if(isAllowed(u)) {
      execute(p)
    }
    else {
      throw new PermissionDenied
    }
  }

  def execute(p:Parameters)
}

/**
 * This trait should be mixed in by MongoBean objects whose actions are
 * to be checked against the original owner of the bean.  The trait will
 * add in an "ownerId" field of type String, and it also makes available
 * to ApiCall declarations the ability to mix the OwnerCheck trait in, 
 * whose purpose is to make sure the User executing the action is the same
 * User who owns this bean.
 */
trait SecuredToOwner {
  self: MongoBean =>

  lazy val ownerId = new Attribute[String]("ownerId")

  trait OwnerCheck extends GenericSecurityCheck {
    override def isAllowed(user: User): Boolean = 
      super.isAllowed(user) &&
      ownerId.value
        .map { id => user.id == id }
        .getOrElse(false)
  }
}
