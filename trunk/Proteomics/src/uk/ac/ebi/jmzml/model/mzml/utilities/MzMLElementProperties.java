package uk.ac.ebi.jmzml.model.mzml.utilities;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Florian Reisinger
 *         Date: 19/12/11
 * @since $version
 */
@XmlRootElement
public class MzMLElementProperties {
    private List<MzMLElementConfig> configurations;


    public List<MzMLElementConfig> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<MzMLElementConfig> configurations) {
        this.configurations = configurations;
    }

}
