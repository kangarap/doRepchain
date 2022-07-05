package rep.sc.tpl

import rep.proto.rc2.ActionResult
import rep.sc.scalax.{ContractContext, ContractException, IContract}
import org.json4s.{DefaultFormats, MappingException}
import org.json4s.jackson.JsonMethods

case class TestData(name: String, age: String)

class TTTTPL extends IContract{

  implicit val formats: DefaultFormats.type = DefaultFormats

  override def init(ctx: ContractContext): Unit = {
    println(s"tid: $ctx.t.id")
  }

  def createValue(ctx: ContractContext, testData: TestData): ActionResult = {
    val name = testData.name;
    val age = testData.age;
    val key = name + age +"112123" + "12Ihbdw";
    ctx.api.setVal(key, testData.age)
    null
  }

  def getValue(ctx: ContractContext, testData: TestData): ActionResult = {
    val name = testData.name;
    val age = testData.age;
    val key = name + age +"12112d21kjbd超呢3";

    val ages = ctx.api.getVal(key)
    println(ages + "==================")
    null
  }

  override def onAction(ctx: ContractContext, action: String, sdata: String): ActionResult = {
    val jsonData = JsonMethods.parse(sdata)

    action match {
      case "createValue" =>
        try {
          val data = jsonData.extract[TestData]
          createValue(ctx, data)
        }catch {
          case mapEx: MappingException => throw ContractException(mapEx.getMessage)
        }


      case "getValue" =>
        try {
          val data = jsonData.extract[TestData]
          getValue(ctx, data)
        } catch {
          case mapEx: MappingException => throw ContractException(mapEx.getMessage)
        }
      case _ => throw ContractException(s"合约中么诶呦")
    }
  }
}
