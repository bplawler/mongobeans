package mongobeans

import com.mongodb.casbah.Imports._

object Config {
  val conn = MongoConnection("mongodb.circupon.internal")
  val db = conn("mongobeans")
  val deals = db("deals")
}

trait MongoBean {
  private var dbObj: Option[DBObject] = None

  def load(_id: String) = 
    dbObj = Config.deals.findOneByID(new org.bson.types.ObjectId(_id))

  class Attribute[A](val fieldName: String) {
    def value: Option[A] =
      dbObj
        .map { obj => Option(obj.get(fieldName).asInstanceOf[A]) }
        .getOrElse { None }

    def value_=(a: A) = ensureDbObj += (fieldName -> extractValue(a)) 
      
    protected def extractValue(a: A) = a match {
      case s: StringEnum => a.toString
      case _ => a.asInstanceOf[AnyRef]
    }

    protected def ensureDbObj: DBObject = 
      dbObj.getOrElse { 
        dbObj = Some(new BasicDBObject)
        dbObj.get
      }
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
      fieldContainer += (rule -> extractValue(value))
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

  def save = dbObj.foreach { Config.deals.save(_) }
}

trait StringEnum 
sealed class Retailer extends StringEnum
case object Walgreens extends Retailer
case object CVS extends Retailer
case object RiteAid extends Retailer

class Deal extends MongoBean {
  val source = new Attribute[String]("source")
  val retailer = new Attribute[Retailer]("retailer")
  val number = new Attribute[Long]("retailer")
  val brands = new Attribute[List[String]]("brands")
  val validTo = new AssertedAttribute[java.util.Date]("validTo")
}
