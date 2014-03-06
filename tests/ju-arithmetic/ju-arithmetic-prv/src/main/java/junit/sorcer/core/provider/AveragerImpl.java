package junit.sorcer.core.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public class AveragerImpl implements Averager {

	private Arithmometer arithmometer = new Arithmometer();

	@SuppressWarnings("rawtypes")
	public Context average(Context context) throws RemoteException,
			ContextException {
		return arithmometer.average(context);
	}

}
