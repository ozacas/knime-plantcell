
package uk.ac.ebi.jdispatcher.soap.wublast;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * Input parameters for the tool
 * 
 * <p>Java class for InputParameters complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InputParameters">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="program" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="exp" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="alignments" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="scores" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="align" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="matrix" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="stats" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sensitivity" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="topcombon" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="viewfilter" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="filter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="strand" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sort" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="transltable" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="stype" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="sequence" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="database" type="{http://soap.jdispatcher.ebi.ac.uk}ArrayOfString"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InputParameters", propOrder = {
    "program",
    "exp",
    "alignments",
    "scores",
    "align",
    "matrix",
    "stats",
    "sensitivity",
    "topcombon",
    "viewfilter",
    "filter",
    "strand",
    "sort",
    "transltable",
    "stype",
    "sequence",
    "database"
})
public class InputParameters {

    @XmlElement(required = true)
    protected String program;
    @XmlElementRef(name = "exp", type = JAXBElement.class)
    protected JAXBElement<String> exp;
    @XmlElementRef(name = "alignments", type = JAXBElement.class)
    protected JAXBElement<Integer> alignments;
    @XmlElementRef(name = "scores", type = JAXBElement.class)
    protected JAXBElement<Integer> scores;
    @XmlElementRef(name = "align", type = JAXBElement.class)
    protected JAXBElement<Integer> align;
    @XmlElementRef(name = "matrix", type = JAXBElement.class)
    protected JAXBElement<String> matrix;
    @XmlElementRef(name = "stats", type = JAXBElement.class)
    protected JAXBElement<String> stats;
    @XmlElementRef(name = "sensitivity", type = JAXBElement.class)
    protected JAXBElement<String> sensitivity;
    @XmlElementRef(name = "topcombon", type = JAXBElement.class)
    protected JAXBElement<String> topcombon;
    @XmlElementRef(name = "viewfilter", type = JAXBElement.class)
    protected JAXBElement<Boolean> viewfilter;
    @XmlElementRef(name = "filter", type = JAXBElement.class)
    protected JAXBElement<String> filter;
    @XmlElementRef(name = "strand", type = JAXBElement.class)
    protected JAXBElement<String> strand;
    @XmlElementRef(name = "sort", type = JAXBElement.class)
    protected JAXBElement<String> sort;
    @XmlElementRef(name = "transltable", type = JAXBElement.class)
    protected JAXBElement<Integer> transltable;
    @XmlElement(required = true)
    protected String stype;
    @XmlElementRef(name = "sequence", type = JAXBElement.class)
    protected JAXBElement<String> sequence;
    @XmlElement(required = true)
    protected ArrayOfString database;

    /**
     * Gets the value of the program property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProgram() {
        return program;
    }

    /**
     * Sets the value of the program property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProgram(String value) {
        this.program = value;
    }

    /**
     * Gets the value of the exp property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getExp() {
        return exp;
    }

    /**
     * Sets the value of the exp property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setExp(JAXBElement<String> value) {
        this.exp = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the alignments property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getAlignments() {
        return alignments;
    }

    /**
     * Sets the value of the alignments property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setAlignments(JAXBElement<Integer> value) {
        this.alignments = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the scores property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getScores() {
        return scores;
    }

    /**
     * Sets the value of the scores property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setScores(JAXBElement<Integer> value) {
        this.scores = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the align property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getAlign() {
        return align;
    }

    /**
     * Sets the value of the align property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setAlign(JAXBElement<Integer> value) {
        this.align = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the matrix property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getMatrix() {
        return matrix;
    }

    /**
     * Sets the value of the matrix property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setMatrix(JAXBElement<String> value) {
        this.matrix = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the stats property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getStats() {
        return stats;
    }

    /**
     * Sets the value of the stats property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setStats(JAXBElement<String> value) {
        this.stats = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the sensitivity property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSensitivity() {
        return sensitivity;
    }

    /**
     * Sets the value of the sensitivity property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSensitivity(JAXBElement<String> value) {
        this.sensitivity = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the topcombon property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTopcombon() {
        return topcombon;
    }

    /**
     * Sets the value of the topcombon property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTopcombon(JAXBElement<String> value) {
        this.topcombon = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the viewfilter property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getViewfilter() {
        return viewfilter;
    }

    /**
     * Sets the value of the viewfilter property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setViewfilter(JAXBElement<Boolean> value) {
        this.viewfilter = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFilter(JAXBElement<String> value) {
        this.filter = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the strand property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getStrand() {
        return strand;
    }

    /**
     * Sets the value of the strand property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setStrand(JAXBElement<String> value) {
        this.strand = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the sort property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSort() {
        return sort;
    }

    /**
     * Sets the value of the sort property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSort(JAXBElement<String> value) {
        this.sort = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the transltable property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getTransltable() {
        return transltable;
    }

    /**
     * Sets the value of the transltable property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setTransltable(JAXBElement<Integer> value) {
        this.transltable = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the stype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStype() {
        return stype;
    }

    /**
     * Sets the value of the stype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStype(String value) {
        this.stype = value;
    }

    /**
     * Gets the value of the sequence property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSequence(JAXBElement<String> value) {
        this.sequence = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the database property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfString }
     *     
     */
    public ArrayOfString getDatabase() {
        return database;
    }

    /**
     * Sets the value of the database property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfString }
     *     
     */
    public void setDatabase(ArrayOfString value) {
        this.database = value;
    }

}
