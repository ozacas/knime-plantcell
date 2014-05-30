//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.05.30 at 08:19:45 AM EST 
//


package au.edu.unimelb.plantcell.servers.mascotee.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Constraints complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Constraints">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="allowed_taxa" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="allowed_protein_mass">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;minLength value="0"/>
 *               &lt;maxLength value="10"/>
 *               &lt;pattern value="^\d*$"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="allow_x_missed_cleavages">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
 *               &lt;minInclusive value="0"/>
 *               &lt;maxInclusive value="9"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="enzyme" type="{http://www.plantcell.unimelb.edu.au/bioinformatics/ns/MascotEE/v2}Enzyme"/>
 *         &lt;element name="peptide_tolerance" type="{http://www.plantcell.unimelb.edu.au/bioinformatics/ns/MascotEE/v2}PeptideTolerance"/>
 *         &lt;element name="msms_tolerance" type="{http://www.plantcell.unimelb.edu.au/bioinformatics/ns/MascotEE/v2}MSMSTolerance"/>
 *         &lt;element name="peptide_charge">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;minLength value="2"/>
 *               &lt;maxLength value="12"/>
 *               &lt;enumeration value="Mr"/>
 *               &lt;enumeration value="1+"/>
 *               &lt;enumeration value="2+"/>
 *               &lt;enumeration value="3+"/>
 *               &lt;enumeration value="4+"/>
 *               &lt;enumeration value="5+"/>
 *               &lt;enumeration value="6+"/>
 *               &lt;enumeration value="7+"/>
 *               &lt;enumeration value="8+"/>
 *               &lt;enumeration value="2+ and 3+"/>
 *               &lt;enumeration value="1+, 2+ and 3+"/>
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
@XmlType(name = "Constraints", propOrder = {
    "allowedTaxa",
    "allowedProteinMass",
    "allowXMissedCleavages",
    "enzyme",
    "peptideTolerance",
    "msmsTolerance",
    "peptideCharge"
})
public class Constraints {

    @XmlElement(name = "allowed_taxa", required = true)
    protected String allowedTaxa;
    @XmlElement(name = "allowed_protein_mass", required = true)
    protected String allowedProteinMass;
    @XmlElement(name = "allow_x_missed_cleavages")
    protected int allowXMissedCleavages;
    @XmlElement(required = true)
    protected String enzyme;
    @XmlElement(name = "peptide_tolerance", required = true)
    protected PeptideTolerance peptideTolerance;
    @XmlElement(name = "msms_tolerance", required = true)
    protected MSMSTolerance msmsTolerance;
    @XmlElement(name = "peptide_charge", required = true)
    protected String peptideCharge;

    /**
     * Gets the value of the allowedTaxa property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllowedTaxa() {
        return allowedTaxa;
    }

    /**
     * Sets the value of the allowedTaxa property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllowedTaxa(String value) {
        this.allowedTaxa = value;
    }

    /**
     * Gets the value of the allowedProteinMass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllowedProteinMass() {
        return allowedProteinMass;
    }

    /**
     * Sets the value of the allowedProteinMass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllowedProteinMass(String value) {
        this.allowedProteinMass = value;
    }

    /**
     * Gets the value of the allowXMissedCleavages property.
     * 
     */
    public int getAllowXMissedCleavages() {
        return allowXMissedCleavages;
    }

    /**
     * Sets the value of the allowXMissedCleavages property.
     * 
     */
    public void setAllowXMissedCleavages(int value) {
        this.allowXMissedCleavages = value;
    }

    /**
     * Gets the value of the enzyme property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEnzyme() {
        return enzyme;
    }

    /**
     * Sets the value of the enzyme property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEnzyme(String value) {
        this.enzyme = value;
    }

    /**
     * Gets the value of the peptideTolerance property.
     * 
     * @return
     *     possible object is
     *     {@link PeptideTolerance }
     *     
     */
    public PeptideTolerance getPeptideTolerance() {
        return peptideTolerance;
    }

    /**
     * Sets the value of the peptideTolerance property.
     * 
     * @param value
     *     allowed object is
     *     {@link PeptideTolerance }
     *     
     */
    public void setPeptideTolerance(PeptideTolerance value) {
        this.peptideTolerance = value;
    }

    /**
     * Gets the value of the msmsTolerance property.
     * 
     * @return
     *     possible object is
     *     {@link MSMSTolerance }
     *     
     */
    public MSMSTolerance getMsmsTolerance() {
        return msmsTolerance;
    }

    /**
     * Sets the value of the msmsTolerance property.
     * 
     * @param value
     *     allowed object is
     *     {@link MSMSTolerance }
     *     
     */
    public void setMsmsTolerance(MSMSTolerance value) {
        this.msmsTolerance = value;
    }

    /**
     * Gets the value of the peptideCharge property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPeptideCharge() {
        return peptideCharge;
    }

    /**
     * Sets the value of the peptideCharge property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPeptideCharge(String value) {
        this.peptideCharge = value;
    }

}
