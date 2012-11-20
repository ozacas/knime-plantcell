
package compbio.data.msa._01._12._2010;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for limit complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="limit">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="preset" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="seqNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="seqLength" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *       &lt;attribute name="isDefault" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "limit", propOrder = {
    "preset",
    "seqNumber",
    "seqLength"
})
public class Limit {

    protected String preset;
    protected int seqNumber;
    protected int seqLength;
    @XmlAttribute(required = true)
    protected boolean isDefault;

    /**
     * Gets the value of the preset property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPreset() {
        return preset;
    }

    /**
     * Sets the value of the preset property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPreset(String value) {
        this.preset = value;
    }

    /**
     * Gets the value of the seqNumber property.
     * 
     */
    public int getSeqNumber() {
        return seqNumber;
    }

    /**
     * Sets the value of the seqNumber property.
     * 
     */
    public void setSeqNumber(int value) {
        this.seqNumber = value;
    }

    /**
     * Gets the value of the seqLength property.
     * 
     */
    public int getSeqLength() {
        return seqLength;
    }

    /**
     * Sets the value of the seqLength property.
     * 
     */
    public void setSeqLength(int value) {
        this.seqLength = value;
    }

    /**
     * Gets the value of the isDefault property.
     * 
     */
    public boolean isIsDefault() {
        return isDefault;
    }

    /**
     * Sets the value of the isDefault property.
     * 
     */
    public void setIsDefault(boolean value) {
        this.isDefault = value;
    }

}
