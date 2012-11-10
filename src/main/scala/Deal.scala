package mongobeans

import com.mongodb.casbah.Imports._

object Config {
  val conn = MongoConnection("mongodb.circupon.internal")
  val db = conn("mongobeans")
  val deals = db("deals")
}

trait MongoBean {
  private var dbObj: Option[BasicDBObject] = None

  class Attribute[A](val fieldName: String) {
    def value: Option[A] =
      dbObj
        .map { obj => Option(obj.get(fieldName).asInstanceOf[A]) }
        .getOrElse { None }

    def value_=(a: A) = 
      dbObj.getOrElse { 
        dbObj = Some(new BasicDBObject)
        dbObj.get
      } += (fieldName -> { 
        a match {
          case s: StringEnum => a.toString
          case _ => a.asInstanceOf[AnyRef]
        }
      })
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
}
