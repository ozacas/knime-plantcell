package au.edu.unimelb.plantcell.servers.msconvertee.endpoints;

import javax.xml.bind.annotation.XmlRootElement;

import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.ProteowizardJobType;

@XmlRootElement
public class ProteowizardJob extends ProteowizardJobType {
	// nothing here, just @XmlRootElement needed by JAXB for correct marshalling/unmarshalling of data
}
