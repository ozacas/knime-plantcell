
package compbio.data.msa._01._12._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for score complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="score">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="method" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ranges" type="{http://msa.data.compbio/01/12/2010/}range" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="scores" type="{http://www.w3.org/2001/XMLSchema}float" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "score", propOrder = {
    "method",
    "ranges",
    "scores"
})
public class Score {

    protected String method;
    @XmlElement(nillable = true)
    protected List<Range> ranges;
    @XmlElement(nillable = true)
    protected List<Float> scores;

    /**
     * Gets the value of the method property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the value of the method property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMethod(String value) {
        this.method = value;
    }

    /**
     * Gets the value of the ranges property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ranges property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRanges().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Range }
     * 
     * 
     */
    public List<Range> getRanges() {
        if (ranges == null) {
            ranges = new ArrayList<Range>();
        }
        return this.ranges;
    }

    /**
     * Gets the value of the scores property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the scores property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getScores().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Float }
     * 
     * 
     */
    public List<Float> getScores() {
        if (scores == null) {
            scores = new ArrayList<Float>();
        }
        return this.scores;
    }

}
