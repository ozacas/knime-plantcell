package au.edu.unimelb.plantcell.servers.mascotee.endpoints;

import javax.jws.WebService;

@WebService
public interface TestAuthService {
	public boolean authOk();
}
