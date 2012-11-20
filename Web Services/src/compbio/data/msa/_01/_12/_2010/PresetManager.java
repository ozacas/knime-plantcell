
package compbio.data.msa._01._12._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for presetManager complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="presetManager">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="runnerClassName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="preset" type="{http://msa.data.compbio/01/12/2010/}preset" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "presetManager", propOrder = {
    "runnerClassName",
    "preset"
})
public class PresetManager {

    @XmlElement(required = true)
    protected String runnerClassName;
    @XmlElement(required = true)
    protected List<Preset> preset;

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
     * Gets the value of the preset property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the preset property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPreset().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Preset }
     * 
     * 
     */
    public List<Preset> getPreset() {
        if (preset == null) {
            preset = new ArrayList<Preset>();
        }
        return this.preset;
    }

}
