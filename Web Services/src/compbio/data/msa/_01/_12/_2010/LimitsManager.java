
package compbio.data.msa._01._12._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for limitsManager complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="limitsManager">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="runnerClassName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="limit" type="{http://msa.data.compbio/01/12/2010/}limit" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "limitsManager", propOrder = {
    "runnerClassName",
    "limit"
})
public class LimitsManager {

    protected String runnerClassName;
    @XmlElement(nillable = true)
    protected List<Limit> limit;

    /**
     * Gets the value of the runnerClassName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRunnerClassName() {
        return runnerClassName;
    }

    /**
     * Sets the value of the runnerClassName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRunnerClassName(String value) {
        this.runnerClassName = value;
    }

    /**
     * Gets the value of the limit property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the limit property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLimit().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Limit }
     * 
     * 
     */
    public List<Limit> getLimit() {
        if (limit == null) {
            limit = new ArrayList<Limit>();
        }
        return this.limit;
    }

}
