
package compbio.data.msa._01._12._2010;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for chunkHolder complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="chunkHolder">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="chunk" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="position" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "chunkHolder", propOrder = {
    "chunk",
    "position"
})
public class ChunkHolder {

    protected String chunk;
    protected long position;

    /**
     * Gets the value of the chunk property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getChunk() {
        return chunk;
    }

    /**
     * Sets the value of the chunk property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setChunk(String value) {
        this.chunk = value;
    }

    /**
     * Gets the value of the position property.
     * 
     */
    public long getPosition() {
        return position;
    }

    /**
     * Sets the value of the position property.
     * 
     */
    public void setPosition(long value) {
        this.position = value;
    }

}
