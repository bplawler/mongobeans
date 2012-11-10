package mongobeans

import com.mongodb.casbah.Imports._
import scala.collection.immutable.Set

object Config {
  val conn = MongoConnection("mongodb.circupon.internal")
  val db = conn("mongobeans")
  val deals = db("deals")
}

trait MongoBean {
  val coll: MongoCollection

  private var dbObj: Option[DBObject] = None

  def load(_id: String) = 
    dbObj = coll.findOneByID(new org.bson.types.ObjectId(_id))

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

  def save = dbObj.foreach { coll.save(_) }
}

trait StringEnum[A] {
  private var mappings = Map[String, A]()
  def apply(s: String): Option[A] = mappings.get(s)
  def unapply(a: A): String = a.toString
  def addMapping(i: StringEnumItem) =
    mappings += (i.toString -> i.asInstanceOf[A])
}

class StringEnumItem(enum: StringEnum[_]) {
  enum.addMapping(this);
}

////  EVERYTHING FROM HERE DOWN IS "APP"

sealed class Retailer extends StringEnumItem(RetailerEnum) 

object RetailerEnum extends StringEnum[Retailer] {
  Walgreens ; case object Walgreens extends Retailer 
  CVS       ; case object CVS extends Retailer
  RiteAid   ; case object RiteAid extends Retailer
}

class Deal extends MongoBean {
  val coll = Config.deals

  val source = new Attribute[String]("source")
  val retailer = new EnumAttribute[Retailer]("retailer", RetailerEnum)
  val number = new Attribute[Long]("number")
  val brands = new Attribute[Set[String]]("brand")
  val zipCodes = new Attribute[Set[String]]("zipCode")
  val validTo = new AssertedAttribute[java.util.Date]("validTo")
}
