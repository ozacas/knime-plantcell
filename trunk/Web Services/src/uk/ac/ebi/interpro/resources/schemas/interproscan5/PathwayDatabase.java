//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.03.06 at 01:00:16 PM EST 
//


package uk.ac.ebi.interpro.resources.schemas.interproscan5;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for pathwayDatabase.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="pathwayDatabase">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="META_CYC"/>
 *     &lt;enumeration value="UNI_PATHWAY"/>
 *     &lt;enumeration value="KEGG"/>
 *     &lt;enumeration value="REACTOME"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "pathwayDatabase")
@XmlEnum
public enum PathwayDatabase {

    META_CYC,
    UNI_PATHWAY,
    KEGG,
    REACTOME;

    public String value() {
        return name();
    }

    public static PathwayDatabase fromValue(String v) {
        return valueOf(v);
    }

}
