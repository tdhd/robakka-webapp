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
  def props(channel: Channel[JsValue]) = Props(new GameAPI(channel))
}

/**
 * TODO
 * 
 * - configure teams via parameters
 * - configure board size
 */
class GameAPI(channel: Channel[JsValue]) extends Actor with ActorLogging {
  import context.dispatcher

  val teams = List(Game.Team(0, io.github.tdhd.robakka.behaviours.RandomBehaviour),
    Game.Team(1, io.github.tdhd.robakka.behaviours.RandomBehaviour))
  val game = context.actorOf(Game.props(teams), "Game")

  override def preStart() = game ! Game.Subscribe(self)
  override def postStop() = {
    game ! Game.Unsubscribe(self)
    context.stop(game)
  }

  def receive = {
    // TODO: convert the world state to compatible json
    case ws: World.State =>
      println(s"received world state from game in play akka")
//      // clean field
//      for (i <- 1 to 55; j <- 1 to 55) {
//        val json: JsValue = JsObject(Seq(
//          "name" -> JsString("Watership Down"),
//          "field" -> JsString(s"#game-${i}-${j}"),
//          "color" -> JsString("white")))
//        channel.push(json)
//      }
      ws.entities.foreach {
        case World.AgentEntity(pos, id, team, health, _, _) =>
          val color = if (team == 0) "green" else "red"
          val json: JsValue = JsObject(Seq(
            "name" -> JsString("Watership Down"),
            "field" -> JsString(s"#game-${pos.row}-${pos.col}"),
            "color" -> JsString(color)))
          channel.push(json)
        case _ =>
      }
  }
}
