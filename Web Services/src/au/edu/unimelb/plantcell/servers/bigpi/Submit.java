
package au.edu.unimelb.plantcell.servers.bigpi;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="protein_sequence_as_fasta" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="organism_type" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "proteinSequenceAsFasta",
    "organismType"
})
@XmlRootElement(name = "submit")
public class Submit {

    @XmlElementRef(name = "protein_sequence_as_fasta", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class, required = false)
    protected JAXBElement<String> proteinSequenceAsFasta;
    @XmlElementRef(name = "organism_type", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class, required = false)
    protected JAXBElement<String> organismType;

    /**
     * Gets the value of the proteinSequenceAsFasta property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getProteinSequenceAsFasta() {
        return proteinSequenceAsFasta;
    }

    /**
     * Sets the value of the proteinSequenceAsFasta property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setProteinSequenceAsFasta(JAXBElement<String> value) {
        this.proteinSequenceAsFasta = value;
    }

    /**
     * Gets the value of the organismType property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getOrganismType() {
        return organismType;
    }

    /**
     * Sets the value of the organismType property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setOrganismType(JAXBElement<String> value) {
        this.organismType = value;
    }

}
