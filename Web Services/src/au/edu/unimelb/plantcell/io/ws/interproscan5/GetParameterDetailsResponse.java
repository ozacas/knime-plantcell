
package au.edu.unimelb.plantcell.io.ws.interproscan5;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="parameterDetails" type="{http://soap.jdispatcher.ebi.ac.uk}wsParameterDetails" minOccurs="0"/>
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
    "parameterDetails"
})
@XmlRootElement(name = "getParameterDetailsResponse")
public class GetParameterDetailsResponse {

    protected WsParameterDetails parameterDetails;

    /**
     * Gets the value of the parameterDetails property.
     * 
     * @return
     *     possible object is
     *     {@link WsParameterDetails }
     *     
     */
    public WsParameterDetails getParameterDetails() {
        return parameterDetails;
    }

    /**
     * Sets the value of the parameterDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link WsParameterDetails }
     *     
     */
    public void setParameterDetails(WsParameterDetails value) {
        this.parameterDetails = value;
    }

}
