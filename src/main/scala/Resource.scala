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

object CacheControl extends Enumeration {
  val CacheOnly, CacheFirst, NoCache = Value
}

trait Resource {
  def name: URL
  def get(policy: CacheControl.Value = CacheControl.CacheFirst): Validation[Throwable, Model]
  def delete: Validation[Throwable, Unit]
  def save(model:Model):Validation[Throwable, Unit]

  //These two methods only work when called on directories
  def createDirectory(model: Model): Validation[Throwable, Unit]
  def create(): Validation[Throwable, Resource]
}

