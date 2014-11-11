package deductions.runtime.uri_classify

import java.net.URL
import akka.http.model._
import akka.actor.ActorSystem
import HttpMethods._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.engine.parsing.HttpHeaderParser
import akka.http.model.HttpHeader

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.async.Async.{ async, await }
import akka.io.IO
import akka.util.Timeout
import akka.http.Http
import akka.actor.{ ActorSystem }
import akka.http.model.{ HttpMethods, HttpEntity, HttpRequest, HttpResponse, Uri }
import akka.stream.{ FlowMaterializer }
import akka.stream.scaladsl.Flow
import akka.pattern.ask
import HttpEntity._
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.http.model._
import akka.http.model.HttpMethods._
import java.net.URL

/* classify URI, leveraging on MIME types in HTTP headers.
MIME categories :
text (including HTML), 
image,
sound, 
video,
xml, and other machine processable stuff
rdf, Json-LD, Turtle and other semantic content 
 */
object SemanticURIGuesser {
  
  abstract class SemanticURIType
  object SemanticURI extends SemanticURIType
  object Application extends SemanticURIType
  object Audio extends SemanticURIType
  object Image extends SemanticURIType
  object Message extends SemanticURIType
  object Multipart extends SemanticURIType
  object Text extends SemanticURIType
  object Video extends SemanticURIType
  object Data extends SemanticURIType
  object Unknown extends SemanticURIType
  
  def guessSemanticURIType(url: String) = {
    val response: Future[HttpResponse] = HttpClient.makeRequest(url, HEAD)
    println("response " + response)
    response.map {
      resp => 
//        resp.status match {
//          case StatusCodes.OK 
//          =>
            semanticURITypeFromHeaders(resp, url)
//          case _ => Unknown // or Future fails
//      }
    }
  }
// .header[headers.Server]
  private def semanticURITypeFromHeaders(
      resp : HttpResponse,
      url: String): SemanticURIType = {
    val headers: Seq[HttpHeader] = resp.headers 
	println( "headers " + headers )
    type ContentType = akka.http.model.headers.`Content-Type`
	val contentType = resp.header[ContentType]
    val optionSemanticURIType: Option[SemanticURIType] = contentType.collectFirst {
      case header =>
      println( "header " + header )
      val mediaType = header.contentType.mediaType
      val semanticURIType: SemanticURIType =
          mediaType match {
                case mt if mt.isApplication => guessHeader(mt, url, Application)
                case mt if mt.isAudio => Audio
                case mt if mt.isImage => Image
                case mt if mt.isMessage => Data
                case mt if mt.isMultipart => Data
                case mt if mt.isText => guessHeader(mt, url, Text)
                case mt if mt.isVideo => Video
                case _ => Unknown // TODO : take in account suffix (jpg, etc; there must a library for this :) )
          }
        semanticURIType
    }
	println( "optionSemanticURIType " + optionSemanticURIType )
    optionSemanticURIType match {
      case Some(semanticURIType) => semanticURIType
      case None => Unknown
    }
  }
  
  private def guessHeader(mt:MediaType, url: String, t:SemanticURIType) : SemanticURIType = {
    val st = mt.subType 
    if(
        st == "rdf+xml" || 
        st == "turtle" ||
        st == "n3" ||
        st == "x-turtle" ) {
      SemanticURI
    } else t
  }
  
//  HttpParser.parse( acceptHeaderTurtlePriority )
//  val acceptHeaderTurtlePriority =
//    "text/n3," +
//      "text/turtle;q=1, " +
//      "application/rdf+xml;q=0.6, " +
//      "text/xml;q=0.5" +
//      ", text/plain;q=0.4" +
//      ", text/xhtml;q=0.35" +
//      ", text/html;q=0.3"
//  val acceptHeaderRDFPriority =
//    "application/rdf+xml;q=1, " +
//      "text/xml;q=0.9" +
//      "text/n3;q=0.6," +
//      "text/turtle;;q=0.6, " +
//      ", text/plain;q=0.4" +
//      ", text/xhtml;q=0.35" +
//      ", text/html;q=0.3"
//      
  /* case class HttpResponse(status: StatusCode = StatusCodes.OK,
                        entity: HttpEntity = HttpEntity.Empty,
                        headers: List[HttpHeader] = Nil,
                        protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`) extends HttpMessage with HttpResponsePart {
   */
}