
package dk.dtu.cbs.ws.ws_common;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for gff complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="gff">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="gff_record" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="seqname">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;pattern value=".*"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="source">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;pattern value=".*"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="feature">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;pattern value=".*"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="start" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *                   &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *                   &lt;element name="score" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *                   &lt;element name="strand">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;enumeration value="+"/>
 *                         &lt;enumeration value="-"/>
 *                         &lt;enumeration value="."/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="frame">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;enumeration value="0"/>
 *                         &lt;enumeration value="1"/>
 *                         &lt;enumeration value="2"/>
 *                         &lt;enumeration value="."/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="comment">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;pattern value=".*"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "gff", propOrder = {
    "gffRecord"
})
public class Gff {

    @XmlElement(name = "gff_record", required = true)
    protected List<Gff.GffRecord> gffRecord;

    /**
     * Gets the value of the gffRecord property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the gffRecord property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGffRecord().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Gff.GffRecord }
     * 
     * 
     */
    public List<Gff.GffRecord> getGffRecord() {
        if (gffRecord == null) {
            gffRecord = new ArrayList<Gff.GffRecord>();
        }
        return this.gffRecord;
    }


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
     *         &lt;element name="seqname">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;pattern value=".*"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="source">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;pattern value=".*"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="feature">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;pattern value=".*"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="start" type="{http://www.w3.org/2001/XMLSchema}integer"/>
     *         &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}integer"/>
     *         &lt;element name="score" type="{http://www.w3.org/2001/XMLSchema}float"/>
     *         &lt;element name="strand">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;enumeration value="+"/>
     *               &lt;enumeration value="-"/>
     *               &lt;enumeration value="."/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="frame">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;enumeration value="0"/>
     *               &lt;enumeration value="1"/>
     *               &lt;enumeration value="2"/>
     *               &lt;enumeration value="."/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="comment">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;pattern value=".*"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
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
        "seqname",
        "source",
        "feature",
        "start",
        "end",
        "score",
        "strand",
        "frame",
        "comment"
    })
    public static class GffRecord {

        @XmlElement(required = true)
        protected String seqname;
        @XmlElement(required = true)
        protected String source;
        @XmlElement(required = true)
        protected String feature;
        @XmlElement(required = true)
        protected BigInteger start;
        @XmlElement(required = true)
        protected BigInteger end;
        protected float score;
        @XmlElement(required = true)
        protected String strand;
        @XmlElement(required = true)
        protected String frame;
        @XmlElement(required = true)
        protected String comment;

        /**
         * Gets the value of the seqname property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getSeqname() {
            return seqname;
        }

        /**
         * Sets the value of the seqname property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setSeqname(String value) {
            this.seqname = value;
        }

        /**
         * Gets the value of the source property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getSource() {
            return source;
        }

        /**
         * Sets the value of the source property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setSource(String value) {
            this.source = value;
        }

        /**
         * Gets the value of the feature property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFeature() {
            return feature;
        }

        /**
         * Sets the value of the feature property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFeature(String value) {
            this.feature = value;
        }

        /**
         * Gets the value of the start property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getStart() {
            return start;
        }

        /**
         * Sets the value of the start property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setStart(BigInteger value) {
            this.start = value;
        }

        /**
         * Gets the value of the end property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getEnd() {
            return end;
        }

        /**
         * Sets the value of the end property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setEnd(BigInteger value) {
            this.end = value;
        }

        /**
         * Gets the value of the score property.
         * 
         */
        public float getScore() {
            return score;
        }

        /**
         * Sets the value of the score property.
         * 
         */
        public void setScore(float value) {
            this.score = value;
        }

        /**
         * Gets the value of the strand property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getStrand() {
            return strand;
        }

        /**
         * Sets the value of the strand property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setStrand(String value) {
            this.strand = value;
        }

        /**
         * Gets the value of the frame property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFrame() {
            return frame;
        }

        /**
         * Sets the value of the frame property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFrame(String value) {
            this.frame = value;
        }

        /**
         * Gets the value of the comment property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getComment() {
            return comment;
        }

        /**
         * Sets the value of the comment property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setComment(String value) {
            this.comment = value;
        }

    }

}
