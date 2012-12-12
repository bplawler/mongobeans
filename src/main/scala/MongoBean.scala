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

  protected def beanId: MongoDBObject = 
    _id.value.map(id => MongoDBObject("_id" -> id))
      .getOrElse(throw new RuntimeException("Attempting to create a " +
        "beanId criteria when the _id has not been set."))

 /**
  * Backing Mongo document for this object.
  */
  protected var dbObj: Option[DBObject] = None

  protected var inMemory: Boolean = true

  protected var attributeMap = Map[String, Attribute[_]]()

  def getAttribute(attrName: String): Option[Attribute[_]] = 
    attributeMap.get(attrName)

 /**
  * Public method that will use the current value of the _id attribute to load
  * a backing document behind this bean.
  */
  def load = {
    inMemory = false
  }

 /**
  * Public method for saving the document to the collection after making
  * changes to it in the app.
  */
  def save = {
    if(!inMemory)
      throw new RuntimeException("Do not call save() on objects that have " +
        "been loaded.  Others may be attempting to change the same object.")
    dbObj.foreach(obj => { 
      coll.save(obj, WriteConcern.Safe) 
      inMemory = false
    })
  }

  protected def ensureDbObject: DBObject = {
    if(!inMemory) {
      // Can't use the _id.value here - that would cause an infinite 
      // recursion since Attribute.value calls this method!  Use the dbObj
      // instance directly instead.
      dbObj
        .map(_.get("_id"))
        .map(coll.findOneByID(_).get)
        .get
    }
    else {
      dbObj.getOrElse { 
        dbObj = Some(new BasicDBObject)
        dbObj.get
      }
    }
  }

  override def toString = 
    List(
      super.toString
    , attributeMap
        .map { nameAndAttribute => 
          "%s: %s".format(
            nameAndAttribute._1
          , nameAndAttribute._2.value
          )
        }
        .mkString(" ")
    )
    .mkString("\n")

 /**
  * Instances of the Attribute class are instantiated by the implementing bean
  * for each attribute that is to be stored in Mongo.  [A] is the type of 
  * the attribute (e.g. String, Date, Long, etc.)
  */
  class Attribute[A](val fieldName: String) {
    attributeMap += fieldName -> this

    def value: Option[A] =
      Option(ensureDbObject.get(fieldName))
        .map {
          case a: AnyRef => a.asInstanceOf[A]
        }

    def value_=(a: A) = setValue(a.asInstanceOf[AnyRef])

    def unset = {
      if(inMemory) {
        ensureDbObject -= fieldName
      }
      else {
        coll.update(beanId, $unset(fieldName))
      }
    }

    protected def setValue(a: AnyRef) = {
      if(inMemory) {
        ensureDbObject += (fieldName -> a)
      }
      else {
        coll.update(beanId, $set(fieldName -> a))
      }
    }
  }

 /**
  * An EnumAttribute is an attribute whose value is an enum that has been
  * defined following the pattern put forth by the MongoBeans project.
  */
  class EnumAttribute[A](fieldName: String, enum: StringEnum[A] )
   extends Attribute[A](fieldName) {
    override def value: Option[A] =
      ensureDbObject.getAs[String](fieldName).flatMap(enum(_))

    override def value_=(a: A) = setValue(enum.unapply(a))
  }

  class SetAttribute[A](fieldName: String) 
   extends Attribute[Set[A]](fieldName) {
    def add(a: A) = value = value.getOrElse(Set()) + a

    def remove(a: A) = value = value.getOrElse(Set()) - a

    override def value: Option[Set[A]] =
      Option(ensureDbObject.get(fieldName))
        .map {
          case l: BasicDBList => l.toSet.asInstanceOf[Set[A]]
          case c: Set[_] => c.asInstanceOf[Set[A]]
        }
  }

  class ListAttribute[A](fieldName: String) 
   extends Attribute[List[A]](fieldName) {
    def add(a: A) = value = value.getOrElse(List()) :+ a

    override def value: Option[List[A]] =
      Option(ensureDbObject.get(fieldName))
        .map {
          case l: BasicDBList => l.toList.asInstanceOf[List[A]]
          case c: List[_] => c.asInstanceOf[List[A]]
        }
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
