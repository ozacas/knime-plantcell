//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.07.07 at 08:09:58 AM EST 
//


package au.edu.unimelb.plantcell.servers.msconvertee.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MS2DenoiseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MS2DenoiseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="peaksInWindow" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="windowWidth" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="multichargeFragmentRelaxation" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MS2DenoiseType", propOrder = {
    "peaksInWindow",
    "windowWidth",
    "multichargeFragmentRelaxation"
})
public class MS2DenoiseType {

    protected Integer peaksInWindow;
    protected Double windowWidth;
    protected Boolean multichargeFragmentRelaxation;

    /**
     * Gets the value of the peaksInWindow property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPeaksInWindow() {
        return peaksInWindow;
    }

    /**
     * Sets the value of the peaksInWindow property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPeaksInWindow(Integer value) {
        this.peaksInWindow = value;
    }

    /**
     * Gets the value of the windowWidth property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getWindowWidth() {
        return windowWidth;
    }

    /**
     * Sets the value of the windowWidth property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setWindowWidth(Double value) {
        this.windowWidth = value;
    }

    /**
     * Gets the value of the multichargeFragmentRelaxation property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMultichargeFragmentRelaxation() {
        return multichargeFragmentRelaxation;
    }

    /**
     * Sets the value of the multichargeFragmentRelaxation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMultichargeFragmentRelaxation(Boolean value) {
        this.multichargeFragmentRelaxation = value;
    }

}
