
package dk.dtu.cbs.ws.ws_tmhmm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import dk.dtu.cbs.ws.ws_common.Sequences;


/**
 * <p>Java class for runService complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="runService">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="parameters">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="graphics" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;enumeration value="yes"/>
 *                         &lt;enumeration value="no"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="sequences" type="{http://www.cbs.dtu.dk/ws/ws-common}sequences"/>
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
@XmlType(name = "runService", propOrder = {
    "parameters"
})
public class RunService {

    @XmlElement(required = true)
    protected RunService.Parameters parameters;

    /**
     * Gets the value of the parameters property.
     * 
     * @return
     *     possible object is
     *     {@link RunService.Parameters }
     *     
     */
    public RunService.Parameters getParameters() {
        return parameters;
    }

    /**
     * Sets the value of the parameters property.
     * 
     * @param value
     *     allowed object is
     *     {@link RunService.Parameters }
     *     
     */
    public void setParameters(RunService.Parameters value) {
        this.parameters = value;
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
     *         &lt;element name="graphics" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;enumeration value="yes"/>
     *               &lt;enumeration value="no"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="sequences" type="{http://www.cbs.dtu.dk/ws/ws-common}sequences"/>
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
        "graphics",
        "sequences"
    })
    public static class Parameters {

        protected String graphics;
        @XmlElement(required = true)
        protected Sequences sequences;

        /**
         * Gets the value of the graphics property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getGraphics() {
            return graphics;
        }

        /**
         * Sets the value of the graphics property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setGraphics(String value) {
            this.graphics = value;
        }

        /**
         * Gets the value of the sequences property.
         * 
         * @return
         *     possible object is
         *     {@link Sequences }
         *     
         */
        public Sequences getSequences() {
            return sequences;
        }

        /**
         * Sets the value of the sequences property.
         * 
         * @param value
         *     allowed object is
         *     {@link Sequences }
         *     
         */
        public void setSequences(Sequences value) {
            this.sequences = value;
        }

    }

}
