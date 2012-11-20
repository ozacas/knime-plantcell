
package dk.dtu.cbs.ws.ws_common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for pollQueue complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="pollQueue">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="job">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="jobid" type="{http://www.cbs.dtu.dk/ws/ws-common}jobid"/>
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
@XmlType(name = "pollQueue", propOrder = {
    "job"
})
public class PollQueue {

    @XmlElement(required = true)
    protected PollQueue.Job job;

    /**
     * Gets the value of the job property.
     * 
     * @return
     *     possible object is
     *     {@link PollQueue.Job }
     *     
     */
    public PollQueue.Job getJob() {
        return job;
    }

    /**
     * Sets the value of the job property.
     * 
     * @param value
     *     allowed object is
     *     {@link PollQueue.Job }
     *     
     */
    public void setJob(PollQueue.Job value) {
        this.job = value;
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
        "jobid"
    })
    public static class Job {

        @XmlElement(required = true)
        protected String jobid;

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

    }

}
