
package au.edu.unimelb.plantcell.io.ws.interproscan5;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * Input parameters for the tool
 * 
 * <p>Java class for InputParameters complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InputParameters">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="goterms" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="pathways" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="appl" type="{http://soap.jdispatcher.ebi.ac.uk}ArrayOfString" minOccurs="0"/>
 *         &lt;element name="sequence" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InputParameters", propOrder = {
    "goterms",
    "pathways",
    "appl",
    "sequence"
})
public class InputParameters {

    @XmlElementRef(name = "goterms", type = JAXBElement.class, required = false)
    protected JAXBElement<Boolean> goterms;
    @XmlElementRef(name = "pathways", type = JAXBElement.class, required = false)
    protected JAXBElement<Boolean> pathways;
    @XmlElementRef(name = "appl", type = JAXBElement.class, required = false)
    protected JAXBElement<ArrayOfString> appl;
    @XmlElementRef(name = "sequence", type = JAXBElement.class, required = false)
    protected JAXBElement<String> sequence;

    /**
     * Gets the value of the goterms property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getGoterms() {
        return goterms;
    }

    /**
     * Sets the value of the goterms property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setGoterms(JAXBElement<Boolean> value) {
        this.goterms = value;
    }

    /**
     * Gets the value of the pathways property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getPathways() {
        return pathways;
    }

    /**
     * Sets the value of the pathways property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setPathways(JAXBElement<Boolean> value) {
        this.pathways = value;
    }

    /**
     * Gets the value of the appl property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfString }{@code >}
     *     
     */
    public JAXBElement<ArrayOfString> getAppl() {
        return appl;
    }

    /**
     * Sets the value of the appl property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfString }{@code >}
     *     
     */
    public void setAppl(JAXBElement<ArrayOfString> value) {
        this.appl = value;
    }

    /**
     * Gets the value of the sequence property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSequence(JAXBElement<String> value) {
        this.sequence = value;
    }

}
