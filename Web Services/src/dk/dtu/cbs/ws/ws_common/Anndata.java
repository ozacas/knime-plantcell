
package dk.dtu.cbs.ws.ws_common;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anndata complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="anndata">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="annsource">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="method">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;pattern value=".*"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="version">
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
 *         &lt;element name="ann" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="sequence" type="{http://www.cbs.dtu.dk/ws/ws-common}sequence"/>
 *                   &lt;element name="annrecords">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="annrecord" maxOccurs="unbounded" minOccurs="0">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;sequence>
 *                                       &lt;element name="feature">
 *                                         &lt;simpleType>
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                                             &lt;pattern value=".*"/>
 *                                           &lt;/restriction>
 *                                         &lt;/simpleType>
 *                                       &lt;/element>
 *                                       &lt;choice>
 *                                         &lt;element name="global" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                         &lt;element name="pos" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *                                         &lt;element name="range">
 *                                           &lt;complexType>
 *                                             &lt;complexContent>
 *                                               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                 &lt;sequence>
 *                                                   &lt;element name="begin" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *                                                   &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *                                                 &lt;/sequence>
 *                                               &lt;/restriction>
 *                                             &lt;/complexContent>
 *                                           &lt;/complexType>
 *                                         &lt;/element>
 *                                       &lt;/choice>
 *                                       &lt;element name="score" maxOccurs="unbounded">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                                                 &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                       &lt;element name="strand" minOccurs="0">
 *                                         &lt;simpleType>
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                                             &lt;enumeration value="+"/>
 *                                             &lt;enumeration value="-"/>
 *                                           &lt;/restriction>
 *                                         &lt;/simpleType>
 *                                       &lt;/element>
 *                                       &lt;element name="frame" minOccurs="0">
 *                                         &lt;simpleType>
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *                                             &lt;enumeration value="0"/>
 *                                             &lt;enumeration value="1"/>
 *                                             &lt;enumeration value="2"/>
 *                                           &lt;/restriction>
 *                                         &lt;/simpleType>
 *                                       &lt;/element>
 *                                       &lt;element name="comment" minOccurs="0">
 *                                         &lt;simpleType>
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                                             &lt;pattern value=".*"/>
 *                                           &lt;/restriction>
 *                                         &lt;/simpleType>
 *                                       &lt;/element>
 *                                     &lt;/sequence>
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
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
@XmlType(name = "anndata", propOrder = {
    "annsource",
    "ann"
})
public class Anndata {

    @XmlElement(required = true)
    protected Anndata.Annsource annsource;
    @XmlElement(required = true)
    protected List<Anndata.Ann> ann;

    /**
     * Gets the value of the annsource property.
     * 
     * @return
     *     possible object is
     *     {@link Anndata.Annsource }
     *     
     */
    public Anndata.Annsource getAnnsource() {
        return annsource;
    }

    /**
     * Sets the value of the annsource property.
     * 
     * @param value
     *     allowed object is
     *     {@link Anndata.Annsource }
     *     
     */
    public void setAnnsource(Anndata.Annsource value) {
        this.annsource = value;
    }

    /**
     * Gets the value of the ann property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ann property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAnn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Anndata.Ann }
     * 
     * 
     */
    public List<Anndata.Ann> getAnn() {
        if (ann == null) {
            ann = new ArrayList<Anndata.Ann>();
        }
        return this.ann;
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
     *         &lt;element name="sequence" type="{http://www.cbs.dtu.dk/ws/ws-common}sequence"/>
     *         &lt;element name="annrecords">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="annrecord" maxOccurs="unbounded" minOccurs="0">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;sequence>
     *                             &lt;element name="feature">
     *                               &lt;simpleType>
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *                                   &lt;pattern value=".*"/>
     *                                 &lt;/restriction>
     *                               &lt;/simpleType>
     *                             &lt;/element>
     *                             &lt;choice>
     *                               &lt;element name="global" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                               &lt;element name="pos" type="{http://www.w3.org/2001/XMLSchema}integer"/>
     *                               &lt;element name="range">
     *                                 &lt;complexType>
     *                                   &lt;complexContent>
     *                                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                       &lt;sequence>
     *                                         &lt;element name="begin" type="{http://www.w3.org/2001/XMLSchema}integer"/>
     *                                         &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}integer"/>
     *                                       &lt;/sequence>
     *                                     &lt;/restriction>
     *                                   &lt;/complexContent>
     *                                 &lt;/complexType>
     *                               &lt;/element>
     *                             &lt;/choice>
     *                             &lt;element name="score" maxOccurs="unbounded">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *                                       &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}float"/>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                             &lt;element name="strand" minOccurs="0">
     *                               &lt;simpleType>
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *                                   &lt;enumeration value="+"/>
     *                                   &lt;enumeration value="-"/>
     *                                 &lt;/restriction>
     *                               &lt;/simpleType>
     *                             &lt;/element>
     *                             &lt;element name="frame" minOccurs="0">
     *                               &lt;simpleType>
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
     *                                   &lt;enumeration value="0"/>
     *                                   &lt;enumeration value="1"/>
     *                                   &lt;enumeration value="2"/>
     *                                 &lt;/restriction>
     *                               &lt;/simpleType>
     *                             &lt;/element>
     *                             &lt;element name="comment" minOccurs="0">
     *                               &lt;simpleType>
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *                                   &lt;pattern value=".*"/>
     *                                 &lt;/restriction>
     *                               &lt;/simpleType>
     *                             &lt;/element>
     *                           &lt;/sequence>
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
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
    @XmlType(name = "", propOrder = {
        "sequence",
        "annrecords"
    })
    public static class Ann {

        @XmlElement(required = true)
        protected Sequence sequence;
        @XmlElement(required = true)
        protected Anndata.Ann.Annrecords annrecords;

        /**
         * Gets the value of the sequence property.
         * 
         * @return
         *     possible object is
         *     {@link Sequence }
         *     
         */
        public Sequence getSequence() {
            return sequence;
        }

        /**
         * Sets the value of the sequence property.
         * 
         * @param value
         *     allowed object is
         *     {@link Sequence }
         *     
         */
        public void setSequence(Sequence value) {
            this.sequence = value;
        }

        /**
         * Gets the value of the annrecords property.
         * 
         * @return
         *     possible object is
         *     {@link Anndata.Ann.Annrecords }
         *     
         */
        public Anndata.Ann.Annrecords getAnnrecords() {
            return annrecords;
        }

        /**
         * Sets the value of the annrecords property.
         * 
         * @param value
         *     allowed object is
         *     {@link Anndata.Ann.Annrecords }
         *     
         */
        public void setAnnrecords(Anndata.Ann.Annrecords value) {
            this.annrecords = value;
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
         *         &lt;element name="annrecord" maxOccurs="unbounded" minOccurs="0">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;sequence>
         *                   &lt;element name="feature">
         *                     &lt;simpleType>
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
         *                         &lt;pattern value=".*"/>
         *                       &lt;/restriction>
         *                     &lt;/simpleType>
         *                   &lt;/element>
         *                   &lt;choice>
         *                     &lt;element name="global" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                     &lt;element name="pos" type="{http://www.w3.org/2001/XMLSchema}integer"/>
         *                     &lt;element name="range">
         *                       &lt;complexType>
         *                         &lt;complexContent>
         *                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                             &lt;sequence>
         *                               &lt;element name="begin" type="{http://www.w3.org/2001/XMLSchema}integer"/>
         *                               &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}integer"/>
         *                             &lt;/sequence>
         *                           &lt;/restriction>
         *                         &lt;/complexContent>
         *                       &lt;/complexType>
         *                     &lt;/element>
         *                   &lt;/choice>
         *                   &lt;element name="score" maxOccurs="unbounded">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
         *                             &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}float"/>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                   &lt;element name="strand" minOccurs="0">
         *                     &lt;simpleType>
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
         *                         &lt;enumeration value="+"/>
         *                         &lt;enumeration value="-"/>
         *                       &lt;/restriction>
         *                     &lt;/simpleType>
         *                   &lt;/element>
         *                   &lt;element name="frame" minOccurs="0">
         *                     &lt;simpleType>
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
         *                         &lt;enumeration value="0"/>
         *                         &lt;enumeration value="1"/>
         *                         &lt;enumeration value="2"/>
         *                       &lt;/restriction>
         *                     &lt;/simpleType>
         *                   &lt;/element>
         *                   &lt;element name="comment" minOccurs="0">
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
        @XmlType(name = "", propOrder = {
            "annrecord"
        })
        public static class Annrecords {

            protected List<Anndata.Ann.Annrecords.Annrecord> annrecord;

            /**
             * Gets the value of the annrecord property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the annrecord property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getAnnrecord().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link Anndata.Ann.Annrecords.Annrecord }
             * 
             * 
             */
            public List<Anndata.Ann.Annrecords.Annrecord> getAnnrecord() {
                if (annrecord == null) {
                    annrecord = new ArrayList<Anndata.Ann.Annrecords.Annrecord>();
                }
                return this.annrecord;
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
             *         &lt;element name="feature">
             *           &lt;simpleType>
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
             *               &lt;pattern value=".*"/>
             *             &lt;/restriction>
             *           &lt;/simpleType>
             *         &lt;/element>
             *         &lt;choice>
             *           &lt;element name="global" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *           &lt;element name="pos" type="{http://www.w3.org/2001/XMLSchema}integer"/>
             *           &lt;element name="range">
             *             &lt;complexType>
             *               &lt;complexContent>
             *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                   &lt;sequence>
             *                     &lt;element name="begin" type="{http://www.w3.org/2001/XMLSchema}integer"/>
             *                     &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}integer"/>
             *                   &lt;/sequence>
             *                 &lt;/restriction>
             *               &lt;/complexContent>
             *             &lt;/complexType>
             *           &lt;/element>
             *         &lt;/choice>
             *         &lt;element name="score" maxOccurs="unbounded">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
             *                   &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}float"/>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *         &lt;element name="strand" minOccurs="0">
             *           &lt;simpleType>
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
             *               &lt;enumeration value="+"/>
             *               &lt;enumeration value="-"/>
             *             &lt;/restriction>
             *           &lt;/simpleType>
             *         &lt;/element>
             *         &lt;element name="frame" minOccurs="0">
             *           &lt;simpleType>
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
             *               &lt;enumeration value="0"/>
             *               &lt;enumeration value="1"/>
             *               &lt;enumeration value="2"/>
             *             &lt;/restriction>
             *           &lt;/simpleType>
             *         &lt;/element>
             *         &lt;element name="comment" minOccurs="0">
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
                "feature",
                "global",
                "pos",
                "range",
                "score",
                "strand",
                "frame",
                "comment"
            })
            public static class Annrecord {

                @XmlElement(required = true)
                protected String feature;
                protected String global;
                protected BigInteger pos;
                protected Anndata.Ann.Annrecords.Annrecord.Range range;
                @XmlElement(required = true)
                protected List<Anndata.Ann.Annrecords.Annrecord.Score> score;
                protected String strand;
                protected BigInteger frame;
                protected String comment;

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
                 * Gets the value of the global property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getGlobal() {
                    return global;
                }

                /**
                 * Sets the value of the global property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setGlobal(String value) {
                    this.global = value;
                }

                /**
                 * Gets the value of the pos property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link BigInteger }
                 *     
                 */
                public BigInteger getPos() {
                    return pos;
                }

                /**
                 * Sets the value of the pos property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link BigInteger }
                 *     
                 */
                public void setPos(BigInteger value) {
                    this.pos = value;
                }

                /**
                 * Gets the value of the range property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Anndata.Ann.Annrecords.Annrecord.Range }
                 *     
                 */
                public Anndata.Ann.Annrecords.Annrecord.Range getRange() {
                    return range;
                }

                /**
                 * Sets the value of the range property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Anndata.Ann.Annrecords.Annrecord.Range }
                 *     
                 */
                public void setRange(Anndata.Ann.Annrecords.Annrecord.Range value) {
                    this.range = value;
                }

                /**
                 * Gets the value of the score property.
                 * 
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the JAXB object.
                 * This is why there is not a <CODE>set</CODE> method for the score property.
                 * 
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getScore().add(newItem);
                 * </pre>
                 * 
                 * 
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link Anndata.Ann.Annrecords.Annrecord.Score }
                 * 
                 * 
                 */
                public List<Anndata.Ann.Annrecords.Annrecord.Score> getScore() {
                    if (score == null) {
                        score = new ArrayList<Anndata.Ann.Annrecords.Annrecord.Score>();
                    }
                    return this.score;
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
                 *     {@link BigInteger }
                 *     
                 */
                public BigInteger getFrame() {
                    return frame;
                }

                /**
                 * Sets the value of the frame property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link BigInteger }
                 *     
                 */
                public void setFrame(BigInteger value) {
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
                 *         &lt;element name="begin" type="{http://www.w3.org/2001/XMLSchema}integer"/>
                 *         &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}integer"/>
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
                    "begin",
                    "end"
                })
                public static class Range {

                    @XmlElement(required = true)
                    protected BigInteger begin;
                    @XmlElement(required = true)
                    protected BigInteger end;

                    /**
                     * Gets the value of the begin property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link BigInteger }
                     *     
                     */
                    public BigInteger getBegin() {
                        return begin;
                    }

                    /**
                     * Sets the value of the begin property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link BigInteger }
                     *     
                     */
                    public void setBegin(BigInteger value) {
                        this.begin = value;
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
                 *         &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
                 *         &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}float"/>
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
                    "key",
                    "value"
                })
                public static class Score {

                    protected String key;
                    protected float value;

                    /**
                     * Gets the value of the key property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    public String getKey() {
                        return key;
                    }

                    /**
                     * Sets the value of the key property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    public void setKey(String value) {
                        this.key = value;
                    }

                    /**
                     * Gets the value of the value property.
                     * 
                     */
                    public float getValue() {
                        return value;
                    }

                    /**
                     * Sets the value of the value property.
                     * 
                     */
                    public void setValue(float value) {
                        this.value = value;
                    }

                }

            }

        }

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
     *         &lt;element name="method">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;pattern value=".*"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="version">
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
        "method",
        "version"
    })
    public static class Annsource {

        @XmlElement(required = true)
        protected String method;
        @XmlElement(required = true)
        protected String version;

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
         * Gets the value of the version property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getVersion() {
            return version;
        }

        /**
         * Sets the value of the version property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setVersion(String value) {
            this.version = value;
        }

    }

}
