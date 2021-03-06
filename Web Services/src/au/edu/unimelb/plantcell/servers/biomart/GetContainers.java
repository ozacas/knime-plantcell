
package au.edu.unimelb.plantcell.servers.biomart;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getContainers complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getContainers">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="datasets" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="config" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="withAttributes" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="withFilters" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="allowPartialList" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getContainers", propOrder = {
    "datasets",
    "config",
    "withAttributes",
    "withFilters",
    "allowPartialList"
})
public class GetContainers {

    protected String datasets;
    protected String config;
    protected Boolean withAttributes;
    protected Boolean withFilters;
    protected Boolean allowPartialList;

    /**
     * Gets the value of the datasets property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatasets() {
        return datasets;
    }

    /**
     * Sets the value of the datasets property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatasets(String value) {
        this.datasets = value;
    }

    /**
     * Gets the value of the config property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConfig() {
        return config;
    }

    /**
     * Sets the value of the config property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConfig(String value) {
        this.config = value;
    }

    /**
     * Gets the value of the withAttributes property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isWithAttributes() {
        return withAttributes;
    }

    /**
     * Sets the value of the withAttributes property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWithAttributes(Boolean value) {
        this.withAttributes = value;
    }

    /**
     * Gets the value of the withFilters property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isWithFilters() {
        return withFilters;
    }

    /**
     * Sets the value of the withFilters property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWithFilters(Boolean value) {
        this.withFilters = value;
    }

    /**
     * Gets the value of the allowPartialList property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAllowPartialList() {
        return allowPartialList;
    }

    /**
     * Sets the value of the allowPartialList property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAllowPartialList(Boolean value) {
        this.allowPartialList = value;
    }

}
