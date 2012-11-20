
package au.edu.unimelb.plantcell.servers.nectar.estscan;

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
 *         &lt;element name="nucleotide_sequence_as_fasta" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="scoring_model" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "nucleotideSequenceAsFasta",
    "scoringModel"
})
@XmlRootElement(name = "submit")
public class Submit {

    @XmlElementRef(name = "nucleotide_sequence_as_fasta", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class)
    protected JAXBElement<String> nucleotideSequenceAsFasta;
    @XmlElementRef(name = "scoring_model", namespace = "http://nectar.plantcell.unimelb.edu.au", type = JAXBElement.class)
    protected JAXBElement<String> scoringModel;

    /**
     * Gets the value of the nucleotideSequenceAsFasta property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getNucleotideSequenceAsFasta() {
        return nucleotideSequenceAsFasta;
    }

    /**
     * Sets the value of the nucleotideSequenceAsFasta property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setNucleotideSequenceAsFasta(JAXBElement<String> value) {
        this.nucleotideSequenceAsFasta = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the scoringModel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getScoringModel() {
        return scoringModel;
    }

    /**
     * Sets the value of the scoringModel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setScoringModel(JAXBElement<String> value) {
        this.scoringModel = ((JAXBElement<String> ) value);
    }

}
