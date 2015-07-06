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
class GameAPI(channel: Channel[JsValue], worldSize: World.Size) extends Actor with ActorLogging {
  import context.dispatcher

  val teams = List(Game.Team(0, io.github.tdhd.robakka.behaviours.FollowPlantBehaviour),
    Game.Team(1, io.github.tdhd.robakka.behaviours.FollowEnemyBehaviour))
  val game = context.actorOf(Game.props(teams, worldSize, gameUpdateInterval = 500 milliseconds), "Game")

  override def preStart() = game ! Game.Subscribe(self)
  override def postStop() = {
    game ! Game.Unsubscribe(self)
    context.stop(game)
  }

  def receive = {
    case state: World.StateContainer =>
      val nAgents = state.world.map {
        case ((i, j), l) => l.filter {
          case a: World.AgentEntity => true
          case _ => false
        }.size
      }.reduce(_ + _)

      println(s"$nAgents agents left in the game")
      // map to entities
      // collect agents only
      // group by team and print
      state.world.flatMap{
        case ((i, j), l) => l
      }.collect {
        case agent: World.AgentEntity => agent
      }.groupBy(_.team).foreach {
        case (team, teamList) => println(s"team $team has ${teamList.size} members")
      }

      val allEntities = state.world.flatMap {
        case ((row, col), entities) =>
          entities.map {
            case World.AgentEntity(pos, id, team, health, _, _) =>
              JsObject(Seq(
                "entityType" -> JsNumber(1),
                "row" -> JsNumber(pos.row),
                "col" -> JsNumber(pos.col),
                "team" -> JsNumber(team)))
            case World.PlantEntity(pos, id, energy, ref) =>
              JsObject(Seq(
                "entityType" -> JsNumber(2),
                "row" -> JsNumber(pos.row),
                "col" -> JsNumber(pos.col),
                "team" -> JsNull))
          }
      }.toList

      val json: JsValue = JsObject(Seq(
        "nAgents" -> JsNumber(nAgents),
        "entities" -> JsArray(allEntities)))

      // notify the channel about the current world state
      channel.push(json)
  }
}
