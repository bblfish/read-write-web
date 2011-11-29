/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * under the MIT licence defined at
 *    http://www.opensource.org/licenses/mit-license.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.w3.readwriteweb.auth

import com.hp.hpl.jena.vocabulary.DCTerms
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import org.w3.readwriteweb.util.trySome
import java.lang.ref.WeakReference
import com.hp.hpl.jena.rdf.model.{Model, Property, ModelFactory}

/**
 * Classes for the tests in WebID Authentication.
 *
 * The idea is to try to use the earl test cases we are defining at the WebID XG as
 * a way of collecting tests done to prove the X509Claim and the WebIDClaim.
 *
 * ( bblfish: theses classes feel very ad-hoc, and it is clearly a first pass:
 *  - we are at the point currently of verifying which tests we need
 *  - one gets the feeling these classes could even contain behavior
 *  - we have instances of one class failing but not necessarily succeeding )
 *
 */


object Tests {
  // This is where the earl tests are documented and named
  val ns = "http://www.w3.org/2005/Incubator/webid/earl/RelyingParty"
  val skos = "http://www.w3.org/2004/02/skos/core#"

  private var m : WeakReference[Model] = null
  
  //todo: this model should be a weak pointer
   def model= {
     if (m==null || m.get() == null) m = new WeakReference( ModelFactory.createDefaultModel().read(
       this.getClass.getResourceAsStream("/ontologies/RelyingParty.n3"),
       ns,  "TURTLE" ) )
     m.get;
   }


}


trait Test {
  val title: String
  val description: String
  val note: String
}

/**
 * Test with extra information taken from the ontologies
 */
class PubTest(val name: String) extends Test {
  import Tests._
  implicit def boolToResult(bool: Boolean): Result = if (bool) passed else failed

  private def value(p: Property) = trySome(resource.getProperty(p).getLiteral.getLexicalForm) getOrElse "-missing-"
  
  val title: String = value(DCTerms.title)
  val description: String = value(DCTerms.description)
  val note: String = value(model.createProperty(skos,"note"))

  private def resource = model.getResource(ns+"#"+name)
}

//todo: (bblfish:) I get the feeling that one could put the logic into the tests directly.
//      would that make things easier or better?

//
//The types of tests that we do here
//

//for X509Claim
object certProvided extends PubTest("certificateProvided")   {
  def test(cert: Option[X509Certificate]): Assertion = cert match {
    case Some(x509) => new Assertion(this,passed,"got certificate") //the subject is the session
    case None => new Assertion(this,failed,"missing certificate")
  }
}

object certOk extends PubTest("certificateOk") {
   def test(x509: X509Claim): List[Assertion] = {
     val res = certDateOk.test(x509)::certProvidedSan.test(x509)::certPubKeyRecognized.test(x509)::Nil
     val problems = res.filter(v => v.result != passed)
     new Assertion(this,problems.length==0,
       if (problems.length==0) "There were no issues with the certificate"
       else "There were some issues with your certificate")::res
   }
}
object certProvidedSan extends PubTest("certificateProvidedSAN") {
  def test(x509: X509Claim) = new Assertion(this,x509.webidclaims.size >0,
    " There are "+x509.webidclaims.size+" SANs in the certificate")
}
object certDateOk extends PubTest("certificateDateOk") {
  def test(x509: X509Claim) = 
    new Assertion(this, x509.isCurrent,
      "the x509certificate " + (
        if (x509.tooEarly) "is not yet valid "
        else if (x509.tooLate) " passed its validity date "
        else " is valid")
    )
  
}

object certPubKeyRecognized extends PubTest("certificatePubkeyRecognised") {
  def test(claim: X509Claim) = {
    val pk = claim.cert.getPublicKey;
    new Assertion(this, pk.isInstanceOf[RSAPublicKey], "We only support RSA Keys at present. " )
  }
}

//for WebIDClaims
object webidClaimTst extends PubTest("webidClaim")
object pubkeyTypeTst extends PubTest("certificatePubkeyRecognised")
object profileGetTst extends PubTest("profileGet")
object profileParseTst extends PubTest("profileWellFormed")
object profileOkTst extends PubTest("profileOk")
object profileWellFormedKeyTst extends PubTest("profileWellFormedPubkey")


/** Assertions on the success of a Test -- sits inside X509Claim or WebIdClaim and from which full verifications can be constituted */
class Assertion( val of: Test,
             val result: Result = untested,
             val msg: String,
             val err: Option[Throwable] = None )


sealed class Result(val name: String)  {
  val earl = "http://www.w3.org/ns/earl#"
  val id = earl+name
}

object untested extends Result("untested")
object passed extends Result("passed")
object failed extends Result("failed")


