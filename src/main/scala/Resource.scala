package org.w3.readwriteweb

import org.w3.readwriteweb.util._

import java.net.URL
import com.hp.hpl.jena.rdf.model._
import scalaz.{Resource => _, _}
import Scalaz._

trait ResourceManager {
  def basePath:String
  def sanityCheck():Boolean
  def resource(url:URL):Resource
}

trait Resource {
  def name: URL
  def get():Validation[Throwable, Model]
  def save(model:Model):Validation[Throwable, Unit]
  def createDirectory(model: Model): Validation[Throwable, Unit]
}

