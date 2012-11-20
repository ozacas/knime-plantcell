
package au.edu.unimelb.plantcell.servers.nectar.targetp;

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
 *         &lt;element name="is_plant" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="cp_cutoff" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="sp_cutoff" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="m_cutoff" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="o_cutoff" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="fasta" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "isPlant",
    "cpCutoff",
    "spCutoff",
    "mCutoff",
    "oCutoff",
    "fasta"
})
@XmlRootElement(name = "submit")
public class Submit {

    @XmlElement(name = "is_plant")
    protected Boolean isPlant;
    @XmlElement(name = "cp_cutoff")
    protected Double cpCutoff;
    @XmlElement(name = "sp_cutoff")
    protected Double spCutoff;
    @XmlElement(name = "m_cutoff")
    protected Double mCutoff;
    @XmlElement(name = "o_cutoff")
    protected Double oCutoff;
    @XmlElementRef(name = "fasta", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class)
    protected JAXBElement<String> fasta;

    /**
     * Gets the value of the isPlant property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsPlant() {
        return isPlant;
    }

    /**
     * Sets the value of the isPlant property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsPlant(Boolean value) {
        this.isPlant = value;
    }

    /**
     * Gets the value of the cpCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getCpCutoff() {
        return cpCutoff;
    }

    /**
     * Sets the value of the cpCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setCpCutoff(Double value) {
        this.cpCutoff = value;
    }

    /**
     * Gets the value of the spCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getSpCutoff() {
        return spCutoff;
    }

    /**
     * Sets the value of the spCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setSpCutoff(Double value) {
        this.spCutoff = value;
    }

    /**
     * Gets the value of the mCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMCutoff() {
        return mCutoff;
    }

    /**
     * Sets the value of the mCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMCutoff(Double value) {
        this.mCutoff = value;
    }

    /**
     * Gets the value of the oCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getOCutoff() {
        return oCutoff;
    }

    /**
     * Sets the value of the oCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setOCutoff(Double value) {
        this.oCutoff = value;
    }

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

}
