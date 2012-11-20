
package uk.ac.ebi.jdispatcher.soap.clustalo;

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
 *         &lt;element name="guidetreeout" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="dismatout" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="dealign" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="mbed" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="mbediteration" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="iterations" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="gtiterations" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="hmmiterations" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="outfmt" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="stype" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "guidetreeout",
    "dismatout",
    "dealign",
    "mbed",
    "mbediteration",
    "iterations",
    "gtiterations",
    "hmmiterations",
    "outfmt",
    "stype",
    "sequence"
})
public class InputParameters {

    @XmlElementRef(name = "guidetreeout", type = JAXBElement.class)
    protected JAXBElement<Boolean> guidetreeout;
    @XmlElementRef(name = "dismatout", type = JAXBElement.class)
    protected JAXBElement<Boolean> dismatout;
    @XmlElementRef(name = "dealign", type = JAXBElement.class)
    protected JAXBElement<Boolean> dealign;
    @XmlElementRef(name = "mbed", type = JAXBElement.class)
    protected JAXBElement<Boolean> mbed;
    @XmlElementRef(name = "mbediteration", type = JAXBElement.class)
    protected JAXBElement<Boolean> mbediteration;
    @XmlElementRef(name = "iterations", type = JAXBElement.class)
    protected JAXBElement<Integer> iterations;
    @XmlElementRef(name = "gtiterations", type = JAXBElement.class)
    protected JAXBElement<Integer> gtiterations;
    @XmlElementRef(name = "hmmiterations", type = JAXBElement.class)
    protected JAXBElement<Integer> hmmiterations;
    @XmlElementRef(name = "outfmt", type = JAXBElement.class)
    protected JAXBElement<String> outfmt;
    @XmlElementRef(name = "stype", type = JAXBElement.class)
    protected JAXBElement<String> stype;
    @XmlElementRef(name = "sequence", type = JAXBElement.class)
    protected JAXBElement<String> sequence;

    /**
     * Gets the value of the guidetreeout property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getGuidetreeout() {
        return guidetreeout;
    }

    /**
     * Sets the value of the guidetreeout property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setGuidetreeout(JAXBElement<Boolean> value) {
        this.guidetreeout = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the dismatout property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getDismatout() {
        return dismatout;
    }

    /**
     * Sets the value of the dismatout property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setDismatout(JAXBElement<Boolean> value) {
        this.dismatout = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the dealign property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getDealign() {
        return dealign;
    }

    /**
     * Sets the value of the dealign property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setDealign(JAXBElement<Boolean> value) {
        this.dealign = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the mbed property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getMbed() {
        return mbed;
    }

    /**
     * Sets the value of the mbed property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setMbed(JAXBElement<Boolean> value) {
        this.mbed = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the mbediteration property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getMbediteration() {
        return mbediteration;
    }

    /**
     * Sets the value of the mbediteration property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setMbediteration(JAXBElement<Boolean> value) {
        this.mbediteration = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the iterations property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getIterations() {
        return iterations;
    }

    /**
     * Sets the value of the iterations property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setIterations(JAXBElement<Integer> value) {
        this.iterations = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the gtiterations property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getGtiterations() {
        return gtiterations;
    }

    /**
     * Sets the value of the gtiterations property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setGtiterations(JAXBElement<Integer> value) {
        this.gtiterations = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the hmmiterations property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getHmmiterations() {
        return hmmiterations;
    }

    /**
     * Sets the value of the hmmiterations property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setHmmiterations(JAXBElement<Integer> value) {
        this.hmmiterations = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the outfmt property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getOutfmt() {
        return outfmt;
    }

    /**
     * Sets the value of the outfmt property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setOutfmt(JAXBElement<String> value) {
        this.outfmt = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the stype property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getStype() {
        return stype;
    }

    /**
     * Sets the value of the stype property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setStype(JAXBElement<String> value) {
        this.stype = ((JAXBElement<String> ) value);
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
        this.sequence = ((JAXBElement<String> ) value);
    }

}
