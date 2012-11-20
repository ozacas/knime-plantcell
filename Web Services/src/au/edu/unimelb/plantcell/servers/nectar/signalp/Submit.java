
package au.edu.unimelb.plantcell.servers.nectar.signalp;

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
 *         &lt;element name="tm_cutoff" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="notm_cutoff" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="best_or_notm" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="length" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="euk_plus_neg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "tmCutoff",
    "notmCutoff",
    "bestOrNotm",
    "length",
    "eukPlusNeg"
})
@XmlRootElement(name = "submit")
public class Submit {

    @XmlElementRef(name = "fasta", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class)
    protected JAXBElement<String> fasta;
    @XmlElement(name = "tm_cutoff")
    protected Double tmCutoff;
    @XmlElement(name = "notm_cutoff")
    protected Double notmCutoff;
    @XmlElement(name = "best_or_notm")
    protected Boolean bestOrNotm;
    protected Integer length;
    @XmlElementRef(name = "euk_plus_neg", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class)
    protected JAXBElement<String> eukPlusNeg;

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
     * Gets the value of the tmCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getTmCutoff() {
        return tmCutoff;
    }

    /**
     * Sets the value of the tmCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setTmCutoff(Double value) {
        this.tmCutoff = value;
    }

    /**
     * Gets the value of the notmCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getNotmCutoff() {
        return notmCutoff;
    }

    /**
     * Sets the value of the notmCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setNotmCutoff(Double value) {
        this.notmCutoff = value;
    }

    /**
     * Gets the value of the bestOrNotm property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBestOrNotm() {
        return bestOrNotm;
    }

    /**
     * Sets the value of the bestOrNotm property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBestOrNotm(Boolean value) {
        this.bestOrNotm = value;
    }

    /**
     * Gets the value of the length property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getLength() {
        return length;
    }

    /**
     * Sets the value of the length property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setLength(Integer value) {
        this.length = value;
    }

    /**
     * Gets the value of the eukPlusNeg property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getEukPlusNeg() {
        return eukPlusNeg;
    }

    /**
     * Sets the value of the eukPlusNeg property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setEukPlusNeg(JAXBElement<String> value) {
        this.eukPlusNeg = ((JAXBElement<String> ) value);
    }

}
