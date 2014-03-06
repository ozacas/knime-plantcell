
package au.edu.unimelb.plantcell.io.ws.interproscan5;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *         &lt;element name="resultTypes" type="{http://soap.jdispatcher.ebi.ac.uk}wsResultTypes"/>
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
    "resultTypes"
})
@XmlRootElement(name = "getResultTypesResponse")
public class GetResultTypesResponse {

    @XmlElement(required = true)
    protected WsResultTypes resultTypes;

    /**
     * Gets the value of the resultTypes property.
     * 
     * @return
     *     possible object is
     *     {@link WsResultTypes }
     *     
     */
    public WsResultTypes getResultTypes() {
        return resultTypes;
    }

    /**
     * Sets the value of the resultTypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link WsResultTypes }
     *     
     */
    public void setResultTypes(WsResultTypes value) {
        this.resultTypes = value;
    }

}
