
package compbio.data.msa._01._12._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for customAnalize complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="customAnalize">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fastaSequences" type="{http://msa.data.compbio/01/12/2010/}fastaSequence" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="options" type="{http://msa.data.compbio/01/12/2010/}option" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "customAnalize", propOrder = {
    "fastaSequences",
    "options"
})
public class CustomAnalize {

    protected List<FastaSequence> fastaSequences;
    protected List<Option> options;

    /**
     * Gets the value of the fastaSequences property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fastaSequences property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFastaSequences().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FastaSequence }
     * 
     * 
     */
    public List<FastaSequence> getFastaSequences() {
        if (fastaSequences == null) {
            fastaSequences = new ArrayList<FastaSequence>();
        }
        return this.fastaSequences;
    }

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

}
