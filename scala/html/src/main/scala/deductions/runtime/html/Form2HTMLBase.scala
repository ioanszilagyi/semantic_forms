package deductions.runtime.html

import deductions.runtime.core.FormModule
import deductions.runtime.utils._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}
import org.joda.time.DateTime

import scalaz._
import Scalaz._
import deductions.runtime.core.HTTPrequest

/** generate HTML from abstract Form : common parts for Display & edition */
private[html] trait Form2HTMLBase[NODE, URI <: NODE]
    extends BasicWidgets
    with CSSClasses
    with RDFPrefixesInterface
    with URIManagement {

  val config: Configuration
  import config._


  def toPlainString(n: NODE): String = n.toString()
  
  type formMod = FormModule[NODE, URI]
  type FormEntry = formMod#Entry

//  lazy val prefixes = new RDFPrefixes[ImplementationSettings.Rdf]
//		  with ImplementationSettings.RDFModule{}
  
  def makeFieldLabel(preceding: formMod#Entry, field: formMod#Entry, editable: Boolean,
      lang:String="en",
      cssForProperty: String = css.cssClasses.formLabelCSSClass )
  (implicit form: FormModule[NODE, URI]#FormSyntax) = {
    // display field label only if different from preceding
    if (preceding.label != field.label)
      // PENDING is it correct HTML5 ?
      <label for={
        field.htmlName
      } class={
        cssForProperty
      } tabindex="-1">
      <a href={field.property.toString()}
    title={
        labelTooltip(field)
      }
    target="_blank"
    style= {"""
    /* Do not show labels like links */
    text-decoration: none;
    color: #000;
    font-weight: bold;
"""}
      draggable="true"
      data-uri-property={field.property.toString()}
      tabindex="-1">{
        val label = field.label
        // hack before implementing real separators
        if (label.contains("----"))
          label.substring(1).replaceAll("-(-)+", "")
        else if (isSeparator(field) )
          label .
          replace("separator_props_From_Subject", I18NMessages.get("separator_props_From_Subject", lang)) .
          replace("separator_props_From_Classes", I18NMessages.get("separator_props_From_Classes", lang)) .
          replace("_", " ")
        else label
      }</a>
      </label>
    else if(editable){
      <label class={
        cssForProperty
      } title={
      field.comment + " - " + field.property
      }> -- </label>
        <div class={css.cssClasses.formAddDivCSSClass}></div>
    }
    else {
      <label class={
        cssForProperty } title={
      field.comment + " - " + field.property
      } tabindex="-1"> -- </label>
    }
  }

  private def labelTooltip(field: formMod#Entry) = {
    val details = if( displayTechnicalSemWebDetails )
        s"""
          property: ${field.property} -
          type: ${field.type_}"""
          else ""
      s"""${field.comment} - $details"""
  }
  
  def isSeparator(field: formMod#Entry) = toPlainString(field.property).contains("separator_props_")

  def message(m: String,lang: String): String = I18NMessages.get(m, lang)

  def isFirstFieldForProperty( field: formMod#Entry )
    (implicit form: FormModule[NODE, URI]#FormSyntax): Boolean = {
    val ff = form.fields
    val previous = Try(ff(ff.indexOf(field) - 1)).toOption
    previous match {
      case Some(fi) => fi.property != field.property
      case None => true
    }
  }

  def makeHTMLIdResourceSelect(re: formMod#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax): String =
    toPlainString(re.property)

  def makeHTML_Id(entry: formMod#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) =
    "f" + form.fields.indexOf(entry)

  /** URL Encode the RDF node */
  def urlEncode(node: Any) = Form2HTML.urlEncode(node) // URLEncoder.encode(node.toString, "utf-8")

  def createHyperlinkString(hrefPrefix: String = config.hrefDisplayPrefix, uri: String, blanknode: Boolean = false): String = {
    if (hrefPrefix === "")
      uri
    else {
      val suffix = if (blanknode) "&blanknode=true" else ""
      hrefPrefix + urlEncode(uri) + suffix
    }
  }

  /** use this instead of createHyperlinkString() */
  def createHyperlinkElement(uri: String, text: String, hrefPrefix: String = config.hrefDisplayPrefix, blanknode: Boolean = false): NodeSeq = {
    <a  href={createHyperlinkString(hrefPrefix, uri, blanknode)} style="color: rgb(44,133,254);">
    {text}</a>
  }

  /** add data- HTML5 Attributes corresponding to triple To XML Element */
  def addTripleAttributesToXMLElement(elem: Elem, entry: FormEntry): Elem = {
    import entry._
    if (entry.property != null) {
      val typ = type_.headOption.getOrElse(null) match {
        case null => ""
        case t    => t.toString()
      }
      addAttributesToXMLElement(elem, Map(
        "data-rdf-subject" -> subject.toString(),
        "data-rdf-property" -> property.toString(),
        "data-rdf-object" -> value.toString(),
        "data-rdf-type" -> typ))
    } else elem
  }

  /** add Attributes To XML Element - Note could be reused */
  private def addAttributesToXMLElement(elem: Elem, config: Map[String, String]): Elem =
		  elem.copy(attributes = config.foldRight(elem.attributes) {
		  case ((k, v), next) => new scala.xml.UnprefixedAttribute(k, v, next)
		  })

  /** make User Info On given Triple entry */
  def makeUserInfoOnTriples(entry: formMod#Entry, lang: String="en") ={
    val userMetadata = entry.metadata
    val timeMetadata = entry.timeMetadata
    val time: String = new DateTime(timeMetadata).toDateTime.toString("dd/MM/yyyy HH:mm")
    if (timeMetadata != -1){
      <span class="sf-local-rdf-link">
        &nbsp;-&nbsp;{ message("modified_by", lang) }
        {
          <a style="text-decoration: underline" href={
            hrefDisplayPrefix +
              makeAbsoluteURIForSaving(userMetadata) }>{ userMetadata }</a>
        }&nbsp;{ message("on_date", lang) }&nbsp;{ time }
      </span>
    }
    else <span></span>
  }

  def firstNODEOrElseEmptyString(set: Iterable[NODE]): String = set.headOption.getOrElse("").toString()

  def isLanguageDataFittingRequest(
    entry:   formMod#Entry,
    request: HTTPrequest): Boolean = {
    entry match {
      case literalEntry: formMod#LiteralEntry =>
        val userLanguage = request.getLanguage()
        val dataLanguage = literalEntry.lang
        userLanguage == dataLanguage ||
          dataLanguage === "en" ||
          dataLanguage === "" ||
          dataLanguage === "No_language"
      case _ => true
    }
  }

}
