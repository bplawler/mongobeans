import mongobeans._

sealed class Retailer extends StringEnumItem(RetailerEnum) 

object RetailerEnum extends StringEnum[Retailer] {
  Walgreens ; case object Walgreens extends Retailer 
  CVS       ; case object CVS extends Retailer
  RiteAid   ; case object RiteAid extends Retailer
}

class Deal extends CircuponMongoBean {
  val coll = Config.deals

  val _id = new Attribute[org.bson.types.ObjectId]("_id")
  val source = new Attribute[String]("source")
  val retailer = new EnumAttribute[Retailer]("retailer", RetailerEnum)
  val number = new Attribute[Long]("number")
  val brands = new Attribute[Set[String]]("brand")
  val zipCodes = new Attribute[Set[String]]("zipCode")
  val validTo = new AssertedAttribute[java.util.Date]("validTo")
  val title = new AssertedAttribute[String]("title")
}
