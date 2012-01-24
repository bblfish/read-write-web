package org.w3.readwriteweb

import auth.X509CertSigner._
import auth.{X509CertSigner, RDFAuthZ, X509view}
import org.w3.readwriteweb.util._

import unfiltered.jetty._
import Console.err
import org.slf4j.{Logger, LoggerFactory}

import org.clapper.argot._
import ArgotConverters._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.weiglewilczek.slf4s.Logging
import java.lang.{Class, String}
import java.net.InetAddress
import java.io.{FileDescriptor, File}
import java.security.{Permission, KeyStore}

trait ReadWriteWebArgs {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  // in Order to be receptive to DNS changes the DNS cache must be set
  java.security.Security.setProperty("networkaddress.cache.ttl" , ""+60*10); //3 minutes
  java.security.Security.setProperty("networkaddress.cache.negative.ttl",""+60*3)


  val postUsageMsg= Some("""
  |PROPERTIES
  |
  | * Keystore properties that need to be set if https is started
  |  -Djetty.ssl.keyStoreType=type : the type of the keystore, JKS by default usually
  |  -Djetty.ssl.keyStore=path : specify path to key store (for https server certificate)
  |  -Djetty.ssl.keyStorePassword=password : specify password for keystore store (optional)
  |
  |NOTES
  |
  |  - Trust stores are not needed because we use the WebID protocol, and client certs are nearly never signed by CAs
  |  - one of --http or --https must be selected
     """.stripMargin);

  val parser = new ArgotParser("read-write-web",postUsage=postUsageMsg)

  val mode = parser.option[RWWMode](List("mode"), "m", "wiki mode: wiki or strict") {
    (sValue, opt) =>
      sValue match {
        case "wiki" => AllResourcesAlreadyExist
        case "strict" => ResourcesDontExistByDefault
        case _ => throw new ArgotConversionException("Option %s: must be either wiki or strict" format (opt.name, sValue))
      }
    }

  val rdfLanguage = parser.option[Lang](List("language"), "l", "RDF language") {
    (sValue, opt) =>
      sValue match {
        case "n3" => N3
        case "turtle" => TURTLE
        case "rdfxml" => RDFXML
        case _ => throw new ArgotConversionException("Option %s: must be either n3, turtle or rdfxml" format (opt.name, sValue))
      }
  }

    val httpPort = parser.option[Int]("http", "Port","start the http server on port")
    val httpsPort = parser.option[Int]("https","port","start the https server on port")

  val rootDirectory = parser.parameter[File]("rootDirectory", "root directory", false) {
    (sValue, opt) => {
      val file = new File(sValue)
      if (! file.exists)
        throw new ArgotConversionException("Option %s: %s must be a valid path" format (opt.name, sValue))
      else
        file
    }
  }

  val signer = {
    val keystore = new File(System.getProperty( "netty.ssl.keyStore")).toURI.toURL
    val ksTpe = System.getProperty("netty.ssl.keyStoreType","JKS")
    val ksPass = System.getProperty("netty.ssl.keyStorePassword")
    val alias = System.getProperty("netty.ssl.keyAlias","selfsigned")
    X509CertSigner( keystore, ksTpe, ksPass,  alias )
  }

  val baseURL = parser.parameter[String]("baseURL", "base URL", false)

}



object ReadWriteWebMain extends ReadWriteWebArgs {

  import unfiltered.filter.Planify

  // regular Java main
  def main(args: Array[String]) {

    try {
      parser.parse(args)
    } catch {
      case e: ArgotUsageException => err.println(e.message); sys.exit(1)
    }


    val filesystem =
      new Filesystem(
        rootDirectory.value.get,
        baseURL.value.get,
        lang=rdfLanguage.value getOrElse RDFXML)(mode.value getOrElse ResourcesDontExistByDefault)
    
    val rww = new ReadWriteWeb[HttpServletRequest,HttpServletResponse] {
      val rm = filesystem
      def manif = manifest[HttpServletRequest]
      override implicit val authz = new RDFAuthZ[HttpServletRequest,HttpServletResponse](filesystem)
    }


    //this is incomplete: we should be able to start both ports.... not sure how to do this yet.
    val service = httpsPort.value match {
      case Some(port) => new HttpsTrustAll(port,"0.0.0.0")
      case None => Http(httpPort.value.get)
    }

    // configures and launches a Jetty server
    service.filter(new FilterLogger(logger)).
      context("/public"){ ctx:ContextBuilder =>
        ctx.resources(ClasspathUtils.fromClasspath("public/").toURI.toURL)
    }.filter(Planify(rww.intent)).
      filter(Planify(x509v.intent)).
      filter(new EchoPlan().plan).run()
    
  }



  object x509v extends X509view[HttpServletRequest,HttpServletResponse] {
    def manif = manifest[HttpServletRequest]
  }

}


