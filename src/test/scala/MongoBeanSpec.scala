package mongobeans

import org.specs2._
import specification.{Before, Context}
import com.mongodb.casbah.Imports._

class MongoBeanSpec extends Specification { def is =            sequential^
  "This is a specification to test the mongobeans functionality"          ^
                                                                          p^
  "Instantiating a new Deal instance should"                              ^
    "allow me to set and get String properties"                           ! e1^
    "but should not save anything to the database"                        ! e2^
    "until the save method is invoked on the bean"                        ! e3^
                                                                          p^
  "The _id attribute has special behavior.  It should"                    ^
    "not be set for new instances of mongobeans"                          ! e4^
    "be automatically updated when a new bean is saved to the collection" ! e5^
    "if set to a value by the application, a call to load() should load"  +
      "the corresponding document behind this bean"                       ! e6^
                                                                          p^
  "Concurrent modifications to different beans with the same backing doc" ^
    "should be immediately available in both beans"                       ! e7^
                                                                          p^
  "Typed enumerations in and out of the database should"                  ^
    "allow enums to be stored like other attributes"                      ! e8^
    "have the same concurrency support as other attributes"               ! e9^
                                                                          p^
  "AssertedAttributes define lots of additional behavior.  They should"   ^
    "allow apps to assert many possible values for a field"               ! e10^
    "allow for one of those rules to be validated before save is called"  ! e11^
    "as well as after save is called"                                     ! e12^
    "allow applications to obtain the name of the validating rule"        !e12a^
    "fail with an exception if a rule that does not exist is validated"   ! e13^
    "allow the application to invalidate a validated field in-memory"     ! e14^
    "allow the application to invalidate a validated field not in-memory" ! e15^
    "if a field is invalidated that was not validated, not complain"      ! e16^
    "have the same concurrency support as other attributes"               ! e17^
                                                                          p^
  "SetAttributes are a special type of attribute that should"             ^
    "allow me to assign a whole Set as the value in-memory"               ! e18^
    "allow me to assign a whole Set as the value in-db"                   ! e19^
    "allow me to add elements one by one to an in-memory Set"             ! e20^
    "allow me to remove elements one by one from an in-memory Set"        ! e21^
    "allow me to add elements one by one to an in-db Set"                 ! e22^
    "allow me to remove elements one by one from an in-db Set"            ! e23^
                                                                          p^
  "ValueConverters, mixins that can change a validated value, should"     ^
    "not affect the value of asserted rules"                              ! e24^
    "should affect the only the validated value of a field"               ! e25^
                                                                          p^
  "save() uses WriteConcern.Safe, which should"                           ^
    "throw an error on duplicate inserts"                                 ! e26^
                                                                          p^
  "The Attribute.unset() method should"                                   ^
    "clear out the underlying document key for in-memory beans"           ! e27^
    "clear out the underlying document key for in-database beans"         ! e28^
    "not complain if I try to unset a field that is not set"              ! e29^
                                                                          p^
  "MongoBeans also provides a method by which attribute instances may"    +
  "be retrieved by name.  The getAttribute() method should"               ^
    "return the correct named attribute for the name passed in"           ! e30^
    "return none when something other than a valid name is provided"      ! e31^
                                                                          p^
  "An attribute may also take the form of a List, which should"           ^
    "allow me to add elements to the list and get them back in order"     ! e32^
    "allow me to save the list and retrieve it later"                     ! e33^
                                                                          p^
  "A LockableSet is a special type of SetAttribute which should"          ^
    "allow me to add elements just like a normal Set"                    ! l.e1^
    "allow me to lock certain elements in the Set"                       ! l.e2^
    "and have those elements immediately added to the Set"               ! l.e3^
    "and not removable from the Set via Set-level assignment"            ! l.e4^
    "and not removable from the Set via remove()"                        ! l.e8^
    "allow me to block certain elements from the Set"                    ! l.e5^
    "and have those elements immediately removed from the Set"           ! l.e6^
    "and not assignable back into the Set via Set-level assignment"      ! l.e7^
    "and not assignable back into the Set via add()"                     ! l.e9^
                                                                          p^
  "A MapAttribute is another type of collection which should"             ^
    "allow me to put and get elements on an in-memory document"          ! m.e1^
    "allow me to put and get elements on a persisted document"           ! m.e2^
    "allow me to remove elements on an in-memory document"               ! m.e3^
    "allow me to remove elements on a persisted document"                ! m.e4^
    "allow me to access the Map before anything has been added"          ! m.e5^
                                                                          p^
  "A bean that extends security code should"                              ^
    "not allow users without permission to execute secured actions"      ! s.e1^
    "allow permitted users to take secured actions"                      ! s.e2^
                                                                           end
    
  object s { // Security stuff.
    implicit val appUser = new AppUser("1")

    def e1 = {
      val d = defaultDeal
      d.SetSource("test")
    } must throwA[PermissionDenied]

    def e2 = {
      val d = defaultDeal
      d.ownerId.value = "1"
      d.SetSource("test")
      d.source.value must_== Some("test")
    }
  }

  object m { // MapAttribute stuff.
    val s1 = new Survey
    val e5 = s1.answers.get("A") must_== None

    s1.answers.put("A", "Hell yes!")
    s1.answers.put("B", "Hell no!")
    val e1 = s1.answers.get("A") must_== Some("Hell yes!")

    s1.answers.remove("B")
    val e3 = s1.answers.get("B") must_== None

    s1.save
    val s2 = new Survey
    s2._id.value = s1._id.value.get
    s2.load

    s2.answers.put("C", "Well, maybe")
    val e2 = s2.answers.get("A") must_== Some("Hell yes!")

    s2.answers.remove("C")
    val e4 = s2.answers.get("C") must_== None
  }

  object l { // LockableSet stuff.
    val d1 = new Deal
    d1.brands.value = Set("Dove", "Olay")
    d1.save

    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load
    val e1 = d2.brands.value must_== Some(Set("Dove", "Olay"))

    d2.brands.lockElement("Dove")
    val e2 = d2.brands.value must_== Some(Set("Dove", "Olay"))

    d2.brands.lockElement("Ivory")
    val e3 = d2.brands.value must_== Some(Set("Dove", "Olay", "Ivory"))

    d2.brands.value = Set("Dove")
    val e4 = d2.brands.value must_== Some(Set("Dove", "Ivory"))

    d2.brands.remove("Dove")
    val e8 = d2.brands.value must_== Some(Set("Dove", "Ivory"))

    d2.brands.blockElement("Cetaphil")
    val e5 = d2.brands.value must_== Some(Set("Dove", "Ivory"))

    d2.brands.blockElement("Olay")
    val e6 = d2.brands.value must_== Some(Set("Dove", "Ivory"))

    d2.brands.value = Set("Dove", "Olay", "Ivory", "Cetaphil")
    val e7 = d2.brands.value must_== Some(Set("Dove", "Ivory"))

    d2.brands.add("Cetaphil")
    val e9 = d2.brands.value must_== Some(Set("Dove", "Ivory"))
  }

  def e1 = {
    val d = defaultDeal
    d.source.value must_== Some("TestSource")
  }

  def e2 = {
    val d = defaultDeal
    Config.deals.count(MongoDBObject()) must_== 0
  }

  def e3 = {
    val d = defaultDeal
    d.href.value = "e3.d"
    d.save
    Config.deals.count(MongoDBObject()) must_== 1
  }

  def e4 = {
    val d = defaultDeal
    !(d._id.value isDefined)
  }
  
  def e5 = {
    val d = defaultDeal
    d.href.value = "e5.d"
    d.save
    d._id.value.isDefined
  }

  def e6 = {
    val d = defaultDeal
    d.href.value = "e6.d"
    d.save
    val _id = d._id.value
    val newDeal = new Deal
    newDeal._id.value = _id.get
    newDeal.load
    newDeal.source.value must_== d.source.value
  }

  def e7 = {
    val d1 = defaultDeal
    d1.save
    val _id = d1._id.value
    val d2 = new Deal
    d2._id.value = _id.get
    d2.load

    d2.number.value = 3
    d1.number.value must_== Some(3)
  }

  def e8 = {
    val d = new Deal
    d.retailer.value = RetailerEnum.Walgreens
    d.retailer.value must_== Some(RetailerEnum.Walgreens)
  }

  def e9 = {
    val d1 = new Deal
    d1.retailer.value = RetailerEnum.Walgreens
    d1.save
    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load
    
    d1.retailer.value = RetailerEnum.CVS
    d2.retailer.value must_== Some(RetailerEnum.CVS)
  }

  def e10 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.href.value = "e10.d"
    d1.save

    List(
      Config.deals.findOne(
        MongoDBObject("_id" -> d1._id.value.get) ++ 
        ("assertions.title.rule 1" $exists true)
      ).isDefined 
    , Config.deals.findOne(
        MongoDBObject("_id" -> d1._id.value.get) ++ 
        ("assertions.title.rule 2" $exists true)
      ).isDefined
    , Config.deals.findOne(
        MongoDBObject("_id" -> d1._id.value.get) ++ 
        ("assertions.title.somethingElse" $exists false)
      ).isDefined 
    ).foldLeft(true)(_ && _)
  }

  def e11 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.title.validate("rule 2")
    d1.href.value = "e11.d1"
    d1.save

    d1.title.value must_== Some("title 2")
  }

  def e12 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.href.value = "e12.d1"
    d1.save

    d1.title.validate("rule 2")
    d1.title.value must_== Some("title 2")
  }

  def e12a = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.href.value = "e12.d1"
    d1.save

    d1.title.validate("rule 2")
    d1.title.validatedBy must_== Some("rule 2")
  }

  def e13 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.save

    d1.title.validate("rule 3")
  } must throwA[RuntimeException]

  def e14 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.title.validate("rule 2")
    d1.title.invalidate
    !d1.title.value.isDefined
  }

  def e15 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.href.value = "e15.d1"
    d1.save

    d1.title.validate("rule 2")
    d1.title.invalidate
    !d1.title.value.isDefined
  }

  def e16 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.href.value = "e16.d1"
    d1.save

    d1.title.invalidate
    !d1.title.value.isDefined
  }

  def e17 = {
    val d1 = new Deal
    d1.title.assertValue("rule 1", "title 1")
    d1.title.assertValue("rule 2", "title 2")
    d1.href.value = "e17.d1"
    d1.save

    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load
    d2.title.getAssertedValue("rule 1") must_== Some("title 1")
  }

  def e18 = {
    val d = new Deal
    d.brands.value = Set("a", "b", "c")
    d.href.value = "e18.d"
    d.save
    d.brands.value must_== Some(Set("a", "b", "c"))
  }

  def e19 = {
    val d1 = new Deal
    d1.brands.value = Set("a", "b", "c")
    d1.href.value = "e19.d1"
    d1.save

    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load

    d1.brands.value = Set("d", "e", "f")
    d2.brands.value must_== Some(Set("d", "e", "f"))
  }

  def e20 = {
    val d = new Deal
    d.brands.add("Dove")
    d.brands.add("Olay")
    d.brands.add("Ivory")

    d.brands.value must_== Some(Set("Dove", "Olay", "Ivory"))
  }

  def e21 = {
    val d = new Deal
    d.brands.add("Dove")
    d.brands.add("Olay")
    d.brands.add("Ivory")

    d.brands.remove("Olay")

    d.brands.value must_== Some(Set("Dove", "Ivory"))
  }

  def e22 = {
    val d1 = new Deal
    d1.brands.add("Dove")
    d1.brands.add("Olay")
    d1.save

    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load

    d1.brands.add("Ivory")
    d2.brands.value must_== Some(Set("Dove", "Olay", "Ivory"))
  }

  def e23 = {
    val d1 = new Deal
    d1.brands.add("Dove")
    d1.brands.add("Olay")
    d1.brands.add("Ivory")
    d1.save

    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load

    d1.brands.remove("Ivory")
    d2.brands.value must_== Some(Set("Dove", "Olay"))
  }

  def e24 = {
    val d1 = new Deal
    d1.caps.assertValue("rule 1", "title 1")
    d1.save

    d1.caps.getAssertedValue("rule 1").get must_== "title 1"
  }

  def e25 = {
    val d1 = new Deal
    d1.caps.assertValue("rule 1", "title 1")
    d1.save
    d1.caps.validate("rule 1")

    d1.caps.value.get must_== "TITLE 1"
  }

  def e26 = {
    val d1 = new Deal
    val d2 = new Deal

    d1.href.value = "dup!"
    d2.href.value = "dup!"

    d1.save
    d2.save
  } must throwA[com.mongodb.MongoException.DuplicateKey]

  def e27 = {
    val d = new Deal
    d.source.value = "The Source"
    d.source.unset
    !d.source.value.isDefined
  }

  def e28 = {
    val d1 = new Deal
    d1.source.value = "The Source"
    d1.save

    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load

    d1.source.unset
    !d2.source.value.isDefined
  }

  def e29 = {
    val d = new Deal
    d.save
    d.source.unset
    !d.source.value.isDefined
  }
  
  def e30 = {
    val d = new Deal
    val attr = d.getAttribute("source")
    attr must beAnInstanceOf[Option[mongobeans.MongoBean$Attribute]]
  }

  def e31 = {
    val d = new Deal
    val attr = d.getAttribute("foo")
    attr must beNone
  }

  def e32 = {
    val d = new Deal
    val history = List("event 1", "event 2", "event 3")
    d.history.value = history

    d.history.value = d.history.value.get :+ "event 4"
    d.history.value must_== 
      Some(List("event 1", "event 2", "event 3", "event 4"))
  }

  def e33 = {
    val d = new Deal
    val history = List("event 1", "event 2", "event 3")
    d.history.value = history
    d.href.value = "e33"
    d.save

    val d2 = new Deal
    d2._id.value = d._id.value.get
    d2.load

    d2.history.value must_== Some(List("event 1", "event 2", "event 3"))
  }

  implicit val before: Context = new Before { 
    def before = Config.deals.remove(MongoDBObject(), WriteConcern.Safe)
  }
    
  def defaultDeal = {
    val d = new Deal 
    d.source.value = "TestSource"
    d
  }
}

