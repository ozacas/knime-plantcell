//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.05.30 at 08:19:45 AM EST 
//


package au.edu.unimelb.plantcell.servers.mascotee.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Reporting complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Reporting">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="overview" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="top">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;minLength value="1"/>
 *               &lt;maxLength value="10"/>
 *               &lt;whiteSpace value="collapse"/>
 *               &lt;enumeration value="5"/>
 *               &lt;enumeration value="10"/>
 *               &lt;enumeration value="20"/>
 *               &lt;enumeration value="30"/>
 *               &lt;enumeration value="50"/>
 *               &lt;enumeration value="100"/>
 *               &lt;enumeration value="200"/>
 *               &lt;enumeration value="AUTO"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Reporting", propOrder = {
    "overview",
    "top"
})
public class Reporting {

    protected boolean overview;
    @XmlElement(required = true)
    protected String top;

    /**
     * Gets the value of the overview property.
     * 
     */
    public boolean isOverview() {
        return overview;
    }

    /**
     * Sets the value of the overview property.
     * 
     */
    public void setOverview(boolean value) {
        this.overview = value;
    }

    /**
     * Gets the value of the top property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTop() {
        return top;
    }

    /**
     * Sets the value of the top property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTop(String value) {
        this.top = value;
    }

}
