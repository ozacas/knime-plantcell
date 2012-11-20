
package au.edu.unimelb.plantcell.servers.netphos;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fasta" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="generic" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="best_only" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="kinase" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="cutoff" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "fasta",
    "generic",
    "bestOnly",
    "kinase",
    "cutoff"
})
@XmlRootElement(name = "submit")
public class Submit {

    @XmlElementRef(name = "fasta", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class)
    protected JAXBElement<String> fasta;
    protected Boolean generic;
    @XmlElement(name = "best_only")
    protected Boolean bestOnly;
    protected Boolean kinase;
    protected Double cutoff;

    /**
     * Gets the value of the fasta property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFasta() {
        return fasta;
    }

    /**
     * Sets the value of the fasta property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFasta(JAXBElement<String> value) {
        this.fasta = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the generic property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isGeneric() {
        return generic;
    }

    /**
     * Sets the value of the generic property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setGeneric(Boolean value) {
        this.generic = value;
    }

    /**
     * Gets the value of the bestOnly property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBestOnly() {
        return bestOnly;
    }

    /**
     * Sets the value of the bestOnly property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBestOnly(Boolean value) {
        this.bestOnly = value;
    }

    /**
     * Gets the value of the kinase property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isKinase() {
        return kinase;
    }

    /**
     * Sets the value of the kinase property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setKinase(Boolean value) {
        this.kinase = value;
    }

    /**
     * Gets the value of the cutoff property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getCutoff() {
        return cutoff;
    }

    /**
     * Sets the value of the cutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setCutoff(Double value) {
        this.cutoff = value;
    }

}
