package omnibus.http.streaming

import akka.actor._
import akka.pattern._

import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import HttpHeaders._
import spray.can.Http
import spray.can.server.Stats

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.Future

import omnibus.http.JsonSupport._
import omnibus.domain.topic._


class HttpTopicStatStream(responder: ActorRef, topic : ActorRef) extends StreamingResponse(responder) {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(3 seconds)	
  
  override def startText = s"~~> Streaming topic statistics\n"

  override def preStart() = {
    super.preStart
    context.system.scheduler.schedule(1.seconds,1.seconds){
      val stats = (topic ? TopicStatProtocol.LiveStats).mapTo[TopicStatisticState]
      stats pipeTo self
    }
  }

  override def receive = ({
    case stat : TopicStatisticState => {
        val nextChunk = MessageChunk("data: "+ formatTopicStats.write(stat) +"\n\n")
        responder ! nextChunk 
    }
  }: Receive) orElse super.receive
}