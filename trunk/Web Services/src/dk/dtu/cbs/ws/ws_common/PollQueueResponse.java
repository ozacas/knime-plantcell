
package dk.dtu.cbs.ws.ws_common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for pollQueueResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="pollQueueResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="queueentry">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="jobid" type="{http://www.cbs.dtu.dk/ws/ws-common}jobid"/>
 *                   &lt;element name="datetime" type="{http://www.cbs.dtu.dk/ws/ws-common}datetime"/>
 *                   &lt;element name="status" type="{http://www.cbs.dtu.dk/ws/ws-common}jobstatus"/>
 *                   &lt;element name="expires" type="{http://www.cbs.dtu.dk/ws/ws-common}datetime" minOccurs="0"/>
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
@XmlType(name = "pollQueueResponse", propOrder = {
    "queueentry"
})
public class PollQueueResponse {

    @XmlElement(required = true)
    protected PollQueueResponse.Queueentry queueentry;

    /**
     * Gets the value of the queueentry property.
     * 
     * @return
     *     possible object is
     *     {@link PollQueueResponse.Queueentry }
     *     
     */
    public PollQueueResponse.Queueentry getQueueentry() {
        return queueentry;
    }

    /**
     * Sets the value of the queueentry property.
     * 
     * @param value
     *     allowed object is
     *     {@link PollQueueResponse.Queueentry }
     *     
     */
    public void setQueueentry(PollQueueResponse.Queueentry value) {
        this.queueentry = value;
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
     *         &lt;element name="jobid" type="{http://www.cbs.dtu.dk/ws/ws-common}jobid"/>
     *         &lt;element name="datetime" type="{http://www.cbs.dtu.dk/ws/ws-common}datetime"/>
     *         &lt;element name="status" type="{http://www.cbs.dtu.dk/ws/ws-common}jobstatus"/>
     *         &lt;element name="expires" type="{http://www.cbs.dtu.dk/ws/ws-common}datetime" minOccurs="0"/>
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
        "jobid",
        "datetime",
        "status",
        "expires"
    })
    public static class Queueentry {

        @XmlElement(required = true)
        protected String jobid;
        @XmlElement(required = true)
        protected String datetime;
        @XmlElement(required = true)
        protected Jobstatus status;
        protected String expires;

        /**
         * Gets the value of the jobid property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getJobid() {
            return jobid;
        }

        /**
         * Sets the value of the jobid property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setJobid(String value) {
            this.jobid = value;
        }

        /**
         * Gets the value of the datetime property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDatetime() {
            return datetime;
        }

        /**
         * Sets the value of the datetime property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDatetime(String value) {
            this.datetime = value;
        }

        /**
         * Gets the value of the status property.
         * 
         * @return
         *     possible object is
         *     {@link Jobstatus }
         *     
         */
        public Jobstatus getStatus() {
            return status;
        }

        /**
         * Sets the value of the status property.
         * 
         * @param value
         *     allowed object is
         *     {@link Jobstatus }
         *     
         */
        public void setStatus(Jobstatus value) {
            this.status = value;
        }

        /**
         * Gets the value of the expires property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getExpires() {
            return expires;
        }

        /**
         * Sets the value of the expires property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setExpires(String value) {
            this.expires = value;
        }

    }

}
