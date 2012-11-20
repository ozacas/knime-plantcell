
package compbio.data.msa._01._12._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for parameter complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="parameter">
 *   &lt;complexContent>
 *     &lt;extension base="{http://msa.data.compbio/01/12/2010/}option">
 *       &lt;sequence>
 *         &lt;element name="possibleValues" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="validValue" type="{http://msa.data.compbio/01/12/2010/}valueConstrain" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "parameter", propOrder = {
    "possibleValues",
    "validValue"
})
public class Parameter
    extends Option
{

    protected List<String> possibleValues;
    protected ValueConstrain validValue;

    /**
     * Gets the value of the possibleValues property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the possibleValues property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPossibleValues().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getPossibleValues() {
        if (possibleValues == null) {
            possibleValues = new ArrayList<String>();
        }
        return this.possibleValues;
    }

    /**
     * Gets the value of the validValue property.
     * 
     * @return
     *     possible object is
     *     {@link ValueConstrain }
     *     
     */
    public ValueConstrain getValidValue() {
        return validValue;
    }

    /**
     * Sets the value of the validValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link ValueConstrain }
     *     
     */
    public void setValidValue(ValueConstrain value) {
        this.validValue = value;
    }

}
