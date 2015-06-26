package api

import io.github.tdhd.robakka._
import language.postfixOps
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.Terminated
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._
import io.github.tdhd.robakka.World.AgentEntity

object GameAPI {
  def props(channel: Channel[JsValue], worldSize: World.Size = World.Size(30, 30)) = Props(new GameAPI(channel, worldSize))
}

/**
 * TODO
 * - configure teams via parameters
 */
class GameAPI(channel: Channel[JsValue], worldSize: World.Size = World.Size(30, 30)) extends Actor with ActorLogging {
  import context.dispatcher

  val teams = List(Game.Team(0, io.github.tdhd.robakka.behaviours.RandomBehaviour),
    Game.Team(1, io.github.tdhd.robakka.behaviours.RandomBehaviour))
  val game = context.actorOf(Game.props(teams, worldSize), "Game")

  override def preStart() = game ! Game.Subscribe(self)
  override def postStop() = {
    game ! Game.Unsubscribe(self)
    context.stop(game)
  }

  //  // TODO: convert the world state to compatible json
  //  def convertWorldStateJson(ws: World.State) = {
  //    ws.entities.map {
  //      case World.AgentEntity(pos, id, team, health, _, _) =>
  //        val color = if (team == 0) "blue" else "red"
  //        val elem = Seq(
  //          "name" -> JsString("Watership Down"),
  //          "field" -> JsString(s"#game-${pos.row}-${pos.col}"),
  //          "color" -> JsString(color))
  //      case World.PlantEntity(pos) =>
  //        val elem = Seq(
  //          "name" -> JsString("Watership Down"),
  //          "field" -> JsString(s"#game-${pos.row}-${pos.col}"),
  //          "color" -> JsString("green"))
  //    }
  //  }

  def refresh() = {
    println(s"received world state from game in play akka")
    // clean field
    for (i <- 0 to worldSize.nRows; j <- 0 to worldSize.nCols) {
      val json: JsValue = JsObject(Seq(
        "name" -> JsString("Watership Down"),
        "field" -> JsString(s"#game-${i}-${j}"),
        "color" -> JsString("white")))
      channel.push(json)
    }
  }

  def receive = {
    case ws: World.State =>
      refresh()

      ws.entities.foreach {
        case World.AgentEntity(pos, id, team, health, _, _) =>
          val color = if (team == 0) "green" else "red"
          val json: JsValue = JsObject(Seq(
            "name" -> JsString("Watership Down"),
            "field" -> JsString(s"#game-${pos.row}-${pos.col}"),
            "color" -> JsString(color)))
          channel.push(json)
        case World.PlantEntity(pos) =>
          val json: JsValue = JsObject(Seq(
            "name" -> JsString("Watership Down"),
            "field" -> JsString(s"#game-${pos.row}-${pos.col}"),
            "color" -> JsString("black")))
          channel.push(json)
        case _ =>
      }
  }
}
