
package dk.dtu.cbs.ws.ws_common;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for jobstatus.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="jobstatus">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="UNKNOWN JOBID"/>
 *     &lt;enumeration value="QUEUED"/>
 *     &lt;enumeration value="REJECTED"/>
 *     &lt;enumeration value="PENDING"/>
 *     &lt;enumeration value="FINISHED"/>
 *     &lt;enumeration value="ACTIVE"/>
 *     &lt;enumeration value="WAITING"/>
 *     &lt;enumeration value="QUEUE DOWN"/>
 *     &lt;enumeration value="DATABASE DOWN"/>
 *     &lt;enumeration value="FAILED"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "jobstatus")
@XmlEnum
public enum Jobstatus {

    @XmlEnumValue("UNKNOWN JOBID")
    UNKNOWN_JOBID("UNKNOWN JOBID"),
    QUEUED("QUEUED"),
    REJECTED("REJECTED"),
    PENDING("PENDING"),
    FINISHED("FINISHED"),
    ACTIVE("ACTIVE"),
    WAITING("WAITING"),
    @XmlEnumValue("QUEUE DOWN")
    QUEUE_DOWN("QUEUE DOWN"),
    @XmlEnumValue("DATABASE DOWN")
    DATABASE_DOWN("DATABASE DOWN"),
    FAILED("FAILED");
    private final String value;

    Jobstatus(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Jobstatus fromValue(String v) {
        for (Jobstatus c: Jobstatus.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
