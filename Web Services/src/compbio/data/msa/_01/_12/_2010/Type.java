
package compbio.data.msa._01._12._2010;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Integer"/>
 *     &lt;enumeration value="Float"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "type")
@XmlEnum
public enum Type {

    @XmlEnumValue("Integer")
    INTEGER("Integer"),
    @XmlEnumValue("Float")
    FLOAT("Float");
    private final String value;

    Type(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Type fromValue(String v) {
        for (Type c: Type.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
