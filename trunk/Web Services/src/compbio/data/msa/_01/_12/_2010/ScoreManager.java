
package compbio.data.msa._01._12._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for scoreManager complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="scoreManager">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="seqScores" type="{http://msa.data.compbio/01/12/2010/}scoreHolder" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "scoreManager", propOrder = {
    "seqScores"
})
public class ScoreManager {

    @XmlElement(nillable = true)
    protected List<ScoreHolder> seqScores;

    /**
     * Gets the value of the seqScores property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the seqScores property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSeqScores().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ScoreHolder }
     * 
     * 
     */
    public List<ScoreHolder> getSeqScores() {
        if (seqScores == null) {
            seqScores = new ArrayList<ScoreHolder>();
        }
        return this.seqScores;
    }

}
