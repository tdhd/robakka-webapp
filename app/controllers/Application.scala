package controllers

import play.api._
import play.api.mvc._
import play.api.libs.EventSource
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import java.io.File
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.FileInputStream
import play.api.libs.iteratee.{ Concurrent, Enumeratee }
import scala.util.Random
import api._
import akka.actor.ActorSystem
import javax.inject._

// https://github.com/matthiasn/sse-chat
// https://github.com/matthiasn/sse-perf

// https://www.playframework.com/documentation/2.4.x/ScalaAkka

/**
 * TODO
 * - need start/stop/configure buttons
 * - scatterplot might be better performing
 * - rename feed
 */

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  // @out: sink of stream
  // @channel: source of stream
  val (out, channel) = Concurrent.broadcast[JsValue]

//  var webSubscribers = List.empty[String]

  val gameAPI = system.actorOf(GameAPI.props(channel), "GameAPI")

  // TODO: this is just a test for d3js
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def game = Action { implicit req =>
//    webSubscribers +:= req.host
//    webSubscribers.foreach(println)
    Ok(views.html.game("robakka")("todo"))
  }

  /** Enumeratee for filtering messages based on room */
  def filter() =
    Enumeratee.filter[JsValue] { json: JsValue => (json \ "name").as[String] == "Watership Down" }

  /** Enumeratee for detecting disconnect of SSE stream */
  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] = {
    Enumeratee.onIterateeDone {
      () =>
        println(addr + " - SSE disconnected")
        // addr == "127.0.0.1"
        // but webSubscribers is list with "localhost:9000" entries
        //webSubscribers = webSubscribers.filterNot(_ == addr)
    }
  }

  /** Serves Server Sent Events over HTTP connection */
  def metricsFeed = Action { implicit req =>
    {
      //      Enumerator.fromStream(new File("/dev/random"), 100)
      //      Ok.feed(Enumerator(json) &> EventSource()).as("text/event-stream")
      //      Ok.chunked(Enumerator("kiki", "foo", "bar").andThen(Enumerator.eof))
      //      Ok.feed(Enumerator(stream) &> EventSource()).as("text/event-stream")
      Ok.feed(out
        // &> Concurrent.buffer(100)
        // &> filter()
        &> connDeathWatch(req.remoteAddress)
        &> EventSource()).as("text/event-stream")
    }
  }
}
