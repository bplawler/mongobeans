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

    def value_=(a: A) = 
      dbObj.getOrElse { 
        dbObj = Some(new BasicDBObject)
        dbObj.get
      } += (fieldName -> extractValue(a)) 
      
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

    def assertValue(rule: String, value: A): Unit = 
      getOrAdd(getOrAdd(ensureDbObj, "assertions"), fieldName) += 
        (rule -> extractValue(value))

    def getAssertedValue(rule: String): Option[A] = 
    /*
      dbObj
        .flatMap(obj => Option(obj.get("assertions").asInstanceOf[DBObject]))
        .flatMap(obj => Option(obj.get(fieldName).asInstanceOf[DBObject]))
        .flatMap(obj => Option(obj.get(rule).asInstanceOf[A]))
        */
      for(
        obj <- dbObj; 
        assertionsContainer <- obj.getAs[DBObject]("assertions");
        fieldContainer <- assertionsContainer.getAs[DBObject](fieldName);
        ruleValue <- Option(fieldContainer.get(rule).asInstanceOf[A])
      ) yield ruleValue

    def getAssertions: Option[Map[String, A]] = None

    def isAsserted: Boolean = false

    def validate(rule: String): Unit = { }

    def isValidated: Boolean = value.isDefined

   /**
    * Value will only return a value if the field has been asserted and
    * validated by the application.  Otherwise None comes back.
    */
    override def value: Option[A] = None
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
