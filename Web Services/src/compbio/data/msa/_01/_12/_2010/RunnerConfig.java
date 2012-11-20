
package compbio.data.msa._01._12._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for runnerConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="runnerConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="options" type="{http://msa.data.compbio/01/12/2010/}option" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="parameters" type="{http://msa.data.compbio/01/12/2010/}parameter" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="prmSeparator" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="runnerClassName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "runnerConfig", propOrder = {
    "options",
    "parameters",
    "prmSeparator",
    "runnerClassName"
})
public class RunnerConfig {

    @XmlElement(nillable = true)
    protected List<Option> options;
    @XmlElement(nillable = true)
    protected List<Parameter> parameters;
    protected String prmSeparator;
    @XmlElement(required = true)
    protected String runnerClassName;

    /**
     * Gets the value of the options property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the options property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOptions().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Option }
     * 
     * 
     */
    public List<Option> getOptions() {
        if (options == null) {
            options = new ArrayList<Option>();
        }
        return this.options;
    }

    /**
     * Gets the value of the parameters property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parameters property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParameters().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Parameter }
     * 
     * 
     */
    public List<Parameter> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<Parameter>();
        }
        return this.parameters;
    }

    /**
     * Gets the value of the prmSeparator property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrmSeparator() {
        return prmSeparator;
    }

    /**
     * Sets the value of the prmSeparator property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrmSeparator(String value) {
        this.prmSeparator = value;
    }

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

}
