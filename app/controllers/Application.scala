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

// https://github.com/matthiasn/sse-chat
// https://github.com/matthiasn/sse-perf

class Application extends Controller {

  // @out: sink of stream
  // @channel: source of stream
  val (out, channel) = Concurrent.broadcast[JsValue]

  def index = Action {
    for (i <- 1 to 3) {
      Random.shuffle(List("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o")).zipWithIndex.foreach {
        elem =>
          val json: JsValue = JsObject(Seq(
            "name" -> JsString("Watership Down"),
            "field" -> JsString(s"#${elem._1}${elem._2 + 1}"),
            "color" -> JsString("green")))
          // push to channel in webapp
          channel.push(json)
          Thread.sleep(100)
      }
    }
    //    channel.end()
    Ok(views.html.index("Your new application is ready."))
  }

  def game = Action {
    Ok(views.html.game("robakka")("todo"))
  }

  /** Enumeratee for filtering messages based on room */
  def filter() =
    Enumeratee.filter[JsValue] { json: JsValue => (json \ "name").as[String] == "Watership Down" }

  /** Enumeratee for detecting disconnect of SSE stream */
  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] =
    Enumeratee.onIterateeDone { () => println(addr + " - SSE disconnected") }

  /** Serves Server Sent Events over HTTP connection */
  def metricsFeed = Action { implicit req =>
    {
      //	Enumerator.fromStream(new File("/dev/random"), 100)
      //      Ok.feed(Enumerator(json) &> EventSource()).as("text/event-stream")
      //Ok.chunked(Enumerator("kiki", "foo", "bar").andThen(Enumerator.eof))
      //	Ok.feed(Enumerator(stream) &> EventSource()).as("text/event-stream")
      Ok.feed(out
        // &> Concurrent.buffer(100)
        &> filter()
        &> connDeathWatch(req.remoteAddress)
        &> EventSource()).as("text/event-stream")
    }
  }
}
