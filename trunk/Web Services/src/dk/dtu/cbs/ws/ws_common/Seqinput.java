
package dk.dtu.cbs.ws.ws_common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for seqinput complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="seqinput">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="parameters">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
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
@XmlType(name = "seqinput", propOrder = {
    "parameters"
})
public class Seqinput {

    @XmlElement(required = true)
    protected Seqinput.Parameters parameters;

    /**
     * Gets the value of the parameters property.
     * 
     * @return
     *     possible object is
     *     {@link Seqinput.Parameters }
     *     
     */
    public Seqinput.Parameters getParameters() {
        return parameters;
    }

    /**
     * Sets the value of the parameters property.
     * 
     * @param value
     *     allowed object is
     *     {@link Seqinput.Parameters }
     *     
     */
    public void setParameters(Seqinput.Parameters value) {
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
        "sequences"
    })
    public static class Parameters {

        @XmlElement(required = true)
        protected Sequences sequences;

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
