
package compbio.data.msa._01._12._2010;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LimitExceededException complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LimitExceededException">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="actualNumberofSequences" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="message" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="numberOfSequencesAllowed" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="sequenceLenghtActual" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="sequenceLenghtAllowed" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LimitExceededException", propOrder = {
    "actualNumberofSequences",
    "message",
    "numberOfSequencesAllowed",
    "sequenceLenghtActual",
    "sequenceLenghtAllowed"
})
public class LimitExceededException {

    protected int actualNumberofSequences;
    protected String message;
    protected int numberOfSequencesAllowed;
    protected int sequenceLenghtActual;
    protected int sequenceLenghtAllowed;

    /**
     * Gets the value of the actualNumberofSequences property.
     * 
     */
    public int getActualNumberofSequences() {
        return actualNumberofSequences;
    }

    /**
     * Sets the value of the actualNumberofSequences property.
     * 
     */
    public void setActualNumberofSequences(int value) {
        this.actualNumberofSequences = value;
    }

    /**
     * Gets the value of the message property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the value of the message property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Gets the value of the numberOfSequencesAllowed property.
     * 
     */
    public int getNumberOfSequencesAllowed() {
        return numberOfSequencesAllowed;
    }

    /**
     * Sets the value of the numberOfSequencesAllowed property.
     * 
     */
    public void setNumberOfSequencesAllowed(int value) {
        this.numberOfSequencesAllowed = value;
    }

    /**
     * Gets the value of the sequenceLenghtActual property.
     * 
     */
    public int getSequenceLenghtActual() {
        return sequenceLenghtActual;
    }

    /**
     * Sets the value of the sequenceLenghtActual property.
     * 
     */
    public void setSequenceLenghtActual(int value) {
        this.sequenceLenghtActual = value;
    }

    /**
     * Gets the value of the sequenceLenghtAllowed property.
     * 
     */
    public int getSequenceLenghtAllowed() {
        return sequenceLenghtAllowed;
    }

    /**
     * Sets the value of the sequenceLenghtAllowed property.
     * 
     */
    public void setSequenceLenghtAllowed(int value) {
        this.sequenceLenghtAllowed = value;
    }

}
