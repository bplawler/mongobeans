package mongobeans

import com.mongodb.casbah.Imports._
import scala.collection.immutable.Set

/**
 * MongoBean is a trait that may be mixed in with a scala object in order 
 * to give that bean the ability to be saved into a Mongo database.
 */
trait MongoBean {
 /**
  * A MongoCollection instance into which this bean is to be stored and 
  * retrieved from.
  */
  val coll: MongoCollection
  
 /**
  * Beans that are to be stored in mongo must define an explicit _id
  * attribute which will be used as the primary key in the document collection.
  */
  val _id: Attribute[_]

 /**
  * Backing Mongo document for this object.
  */
  private var dbObj: Option[DBObject] = None

 /**
  * Public method that will use the current value of the _id attribute to load
  * a backing document behind this bean.
  */
  def load = dbObj = coll.findOneByID(_id.value)

 /**
  * Public method for saving the document to the collection after making
  * changes to it in the app.
  */
  def save = dbObj.foreach { coll.save(_) }

 /**
  * Instances of the Attribute class are instantiated by the implementing bean
  * for each attribute that is to be stored in Mongo.  [A] is the type of 
  * the attribute (e.g. String, Date, Long, etc.)
  */
  class Attribute[A](val fieldName: String) {
    def value: Option[A] =
      dbObj
        .flatMap { obj => 
          Option(obj.get(fieldName)) 
          .map {
            case l: BasicDBList => l.toSet.asInstanceOf[A]
            case a: AnyRef => a.asInstanceOf[A]
          }
        }

    def value_=(a: A) = ensureDbObj += (fieldName -> a.asInstanceOf[AnyRef])
      
    protected def ensureDbObj: DBObject = 
      dbObj.getOrElse { 
        dbObj = Some(new BasicDBObject)
        dbObj.get
      }
  }

 /**
  * An EnumAttribute is an attribute whose value is an enum that has been
  * defined following the pattern put forth by the MongoBeans project.
  */
  class EnumAttribute[A](fieldName: String, enum: StringEnum[A] )
   extends Attribute[A](fieldName) {
    override def value: Option[A] =
      dbObj
        .flatMap { obj => 
          obj.getAs[String](fieldName).flatMap(enum(_))
        }

    override def value_=(a: A) = ensureDbObj += (fieldName -> enum.unapply(a))
  }

 /**
  * An AssertedAttribute is an attribute that may have any of several values,
  * and it is up to the application to decide which value is the correct value.
  * When an object that contains assertable attributes is being created, it may
  * not be immediately apparent what the correct value is for the asserted 
  * attribute.  So what we do is add in, based on a set of rules, all of the
  * values that the attribute may have then we leave the downstream processing 
  * of figuring out which value is correct for later.  The underlying mongo 
  * document will store all of the assertions in a separate area.  The only
  * way to actually set the value of the field is to validate one of the 
  * asserted rules.
  */
  class AssertedAttribute[A](fieldName: String) 
   extends Attribute[A](fieldName) {
    private def getOrAdd(o: DBObject, elementName: String): DBObject = 
      o.getAs[DBObject](elementName)
        .getOrElse { 
          val result = new BasicDBObject
          o += (elementName -> result)
          result
        } 

    def assertValue(rule: String, value: A): Unit = {
      val assertionContainer = getOrAdd(ensureDbObj, "assertions")
      val fieldContainer = getOrAdd(assertionContainer, fieldName)
      fieldContainer += (rule -> value.asInstanceOf[AnyRef])
    }

    def getAssertedValue(rule: String): Option[A] = 
      for(
        obj <- dbObj; 
        assertionsContainer <- obj.getAs[DBObject]("assertions");
        fieldContainer <- assertionsContainer.getAs[DBObject](fieldName);
        ruleValue <- Option(fieldContainer.get(rule).asInstanceOf[A])
      ) yield ruleValue

    def getAssertedValues: Option[scala.collection.Map[String, A]] = 
      for(
        obj <- dbObj; 
        assertionsContainer <- obj.getAs[DBObject]("assertions");
        fieldContainer <- assertionsContainer.getAs[DBObject](fieldName)
      ) yield fieldContainer.mapValues { _.asInstanceOf[A] }

    def isAsserted: Boolean = getAssertedValues.isDefined

    def validate(rule: String): Unit = {
      val validationsContainer = getOrAdd(ensureDbObj, "validations")
      val value = getAssertedValue(rule);
      validationsContainer += (fieldName -> rule)
      ensureDbObj += (fieldName -> value.asInstanceOf[Object])
    }

    def invalidate: Unit = {
      val validationsContainer = getOrAdd(ensureDbObj, "validations")
      validationsContainer -= fieldName
      ensureDbObj -= fieldName 
    }

    def isValidated: Boolean = value.isDefined

    override def value_=(v: A) = 
      throw new RuntimeException("The value of an asserted field may not " +
        "be set directly.  It must be asserted and validated.")
  }
}

/**
 * Objects that extend StringEnum may be passed into the EnumAttribute 
 * constructor to provide advice on how to get enumerated values into and out
 * of the database.
 */
trait StringEnum[A] {
  private var mappings = Map[String, A]()
  def apply(s: String): Option[A] = mappings.get(s)
  def unapply(a: A): String = a.toString
  def addMapping(i: StringEnumItem) =
    mappings += (i.toString -> i.asInstanceOf[A])
}

/**
 * Items in an enumeration should extend from a sealed base class, which 
 * itself should extends from this base class.
 */
class StringEnumItem(enum: StringEnum[_]) {
  enum.addMapping(this);
}
