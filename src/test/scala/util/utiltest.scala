package org.w3.readwriteweb

import java.net.URL
import javax.servlet._
import javax.servlet.http._
import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import java.io._
import scala.io.Source

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._

import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import dispatch._

import org.specs.matcher.Matcher

import org.w3.readwriteweb.util._

package object utiltest {
  
  def baseURI(req: Request): URL = new URL("%s%s" format (req.host, req.path))
  
  def beIsomorphicWith(that: Model): Matcher[Model] =
    new Matcher[Model] {
      def apply(otherModel:  => Model) =
        (that isIsomorphicWith otherModel,
         "Model A is isomorphic to model B",
         "%s not isomorphic with %s" format (otherModel.toString, that.toString))
  }
  
  class RequestW(req: Request) {

    def as_model(base: URL, lang: Lang): Handler[Model] =
      req >> { is => modelFromInputStream(is, base, lang).toOption.get }

    def post(body: String, lang: Lang): Request =
      post(body, lang.contentType)
    
    def postSPARQL(body: String): Request =
      post(body, Post.SPARQL)
      
    private def post(body: String, contentType: String): Request =
      (req <:< Map("Content-Type" -> contentType) <<< body).copy(method="POST")

      
    def put(lang: Lang, body: String): Request =
      req <:< Map("Content-Type" -> lang.contentType) <<< body
      
    def get_statusCode: Handler[Int] = new Handler(req, (c, r, e) => c, { case t => () })
    
    def get_header(header: String): Handler[String] = req >:> { _(header).head }

    def get: Request = req.copy(method="GET")

    def get(lang: Lang): Request = req.copy(method="GET") <:< Map("Accept"->lang.contentType)

    def delete: Request = req.copy(method="DELETE")
    
    def >++ [A, B, C] (block:  Request => (Handler[A], Handler[B], Handler[C])) = {
      Handler(req, { (code, res, opt_ent) =>
        val (a, b, c) = block( /\ )
          (a.block(code, res, opt_ent), b.block(code,res,opt_ent), c.block(code,res,opt_ent))
      } )
    }
    
    def >+ [A, B] (block:  Request => (Handler[A], Handler[B])) = {
      Handler(req, { (code, res, opt_ent) =>
        val (a, b) = block( /\ )
        (a.block(code, res, opt_ent), b.block(code,res,opt_ent))
      } )
    }
    
  }
  
  implicit def wrapRequest(req: Request): RequestW = new RequestW(req)
  




}