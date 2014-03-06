
package au.edu.unimelb.plantcell.io.ws.interproscan5;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * A set of parameters passed to a renderer when formatting the data
 * 
 * <p>Java class for wsRawOutputParameters complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="wsRawOutputParameters">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="parameter" type="{http://soap.jdispatcher.ebi.ac.uk}wsRawOutputParameter" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wsRawOutputParameters", propOrder = {
    "parameter"
})
public class WsRawOutputParameters {

    @XmlElement(required = true)
    protected List<WsRawOutputParameter> parameter;

    /**
     * Gets the value of the parameter property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parameter property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParameter().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link WsRawOutputParameter }
     * 
     * 
     */
    public List<WsRawOutputParameter> getParameter() {
        if (parameter == null) {
            parameter = new ArrayList<WsRawOutputParameter>();
        }
        return this.parameter;
    }

}
