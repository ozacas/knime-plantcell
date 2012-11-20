
package compbio.data.msa._01._12._2010;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for jobStatus.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="jobStatus">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="PENDING"/>
 *     &lt;enumeration value="RUNNING"/>
 *     &lt;enumeration value="CANCELLED"/>
 *     &lt;enumeration value="FINISHED"/>
 *     &lt;enumeration value="FAILED"/>
 *     &lt;enumeration value="UNDEFINED"/>
 *     &lt;enumeration value="STARTED"/>
 *     &lt;enumeration value="SUBMITTED"/>
 *     &lt;enumeration value="COLLECTED"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "jobStatus")
@XmlEnum
public enum JobStatus {

    PENDING,
    RUNNING,
    CANCELLED,
    FINISHED,
    FAILED,
    UNDEFINED,
    STARTED,
    SUBMITTED,
    COLLECTED;

    public String value() {
        return name();
    }

    public static JobStatus fromValue(String v) {
        return valueOf(v);
    }

}
