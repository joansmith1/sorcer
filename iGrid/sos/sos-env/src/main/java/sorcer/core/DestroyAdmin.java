package sorcer.core;

import java.rmi.RemoteException;

/**
 *  Interface used to identify a Sorcer Provider
 * @author Pawel Rubach
 */
public interface DestroyAdmin {
	/**
	 * Destroy the service, if possible, including its persistent storage.
	 * 
	 * @see sorcer.core.ServiceProvider#destroy()
	 */
	void destroy() throws RemoteException;

	/**
	 * Destroy all services in this node (virtual machine) by calling each
	 * destroy().
	 * 
	 * @see sorcer.core.ServiceProvider#destroy()
	 */
	void destroyNode() throws RemoteException;

}