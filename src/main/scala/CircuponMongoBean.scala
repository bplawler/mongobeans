package mongobeans

import com.mongodb.casbah.Imports._

trait ValueConverter[A] {
  protected def makeValidatedValue(value: A): A = value
}

trait CircuponMongoBean extends MongoBean {

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
   extends Attribute[A](fieldName) with ValueConverter[A] {

    def getAssertedValue(rule: String): Option[A] = 
      for(
        assertions <- ensureDbObject.getAs[DBObject]("assertions");
        field      <- assertions.getAs[DBObject](fieldName);
        ruleValue  <- Option(field.get(rule).asInstanceOf[A])
      ) yield ruleValue

    def getAssertedValues: Option[scala.collection.Map[String, A]] = 
      for(
        assertions  <- ensureDbObject.getAs[DBObject]("assertions");
        field       <- assertions.getAs[DBObject](fieldName)
      ) yield field.mapValues { _.asInstanceOf[A] }

    def isAsserted: Boolean = getAssertedValues.isDefined

    def isValidated: Boolean = value.isDefined
    
    def validatedBy: Option[String] = 
      for(
        validations <- ensureDbObject.getAs[DBObject]("validations");
        rule        <- validations.getAs[String](fieldName)
      ) yield rule

    override def value_=(v: A): Unit = 
      throw new RuntimeException("The value of an asserted field may not " +
        "be set directly.  It must be asserted and validated.")

   /* 
    * The following are fields that change the document state in some
    * way and thus have implications on application concurrency.
    */

    def assertValue(rule: String, value: A): Unit = {
      if(inMemory) {
        val assertionContainer = getOrAdd(ensureDbObject, "assertions")
        val fieldContainer = getOrAdd(assertionContainer, fieldName)
        fieldContainer += (rule -> value.asInstanceOf[AnyRef])
      }
      else {
        coll.update(beanId, 
          $set(Seq("assertions.%s.%s".format(fieldName, rule) -> value)))
        flush
      }
    }

    def validate(rule: String): Unit = {
      getAssertedValue(rule)
        .map(makeValidatedValue(_))
        .map(validatedValue => {
          if(inMemory) {
            val validationsContainer = getOrAdd(ensureDbObject, "validations")
            validationsContainer += (fieldName -> rule)
            ensureDbObject += (fieldName -> validatedValue.asInstanceOf[Object])
          }
          else {
            coll.update(beanId, 
              $set(Seq("validations.%s".format(fieldName) -> rule)))
            super.value = validatedValue
          }
        }).orElse(
          throw new RuntimeException(
            "rule [%s] has not been asserted on field [%s]"
            .format(rule, fieldName)))
    }

    def invalidate: Unit = {
      if(inMemory) {
        val validationsContainer = getOrAdd(ensureDbObject, "validations")
        validationsContainer -= fieldName
        ensureDbObject -= fieldName 
      }
      else {
        coll.update(beanId, $unset(Seq("validations.%s".format(fieldName))))
        unset
      }
    }
  }
}
