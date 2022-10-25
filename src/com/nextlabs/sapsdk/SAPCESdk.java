package com.nextlabs.sapsdk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.destiny.agent.pdpapi.IPDPApplication;
import com.bluejungle.destiny.agent.pdpapi.IPDPEnforcement;
import com.bluejungle.destiny.agent.pdpapi.IPDPHost;
import com.bluejungle.destiny.agent.pdpapi.IPDPNamedAttributes;
import com.bluejungle.destiny.agent.pdpapi.IPDPResource;
import com.bluejungle.destiny.agent.pdpapi.IPDPSDKCallback;
import com.bluejungle.destiny.agent.pdpapi.IPDPUser;
import com.bluejungle.destiny.agent.pdpapi.PDPApplication;
import com.bluejungle.destiny.agent.pdpapi.PDPException;
import com.bluejungle.destiny.agent.pdpapi.PDPHost;
import com.bluejungle.destiny.agent.pdpapi.PDPNamedAttributes;
import com.bluejungle.destiny.agent.pdpapi.PDPResource;
import com.bluejungle.destiny.agent.pdpapi.PDPSDK;
import com.bluejungle.destiny.agent.pdpapi.PDPTimeout;
import com.bluejungle.destiny.agent.pdpapi.PDPUser;
import com.nextlabs.sapsdk.vo.CEApplication;
import com.nextlabs.sapsdk.vo.CEAttribute;
import com.nextlabs.sapsdk.vo.CEAttributes;
import com.nextlabs.sapsdk.vo.CEEnforcement;
import com.nextlabs.sapsdk.vo.CEEnforcement.CEResponse;
import com.nextlabs.sapsdk.vo.CENamedAttributes;
import com.nextlabs.sapsdk.vo.CERequest;
import com.nextlabs.sapsdk.vo.CEResource;
import com.nextlabs.sapsdk.vo.CESdkException;
import com.nextlabs.sapsdk.vo.CESdkTimeoutException;
import com.nextlabs.sapsdk.vo.CEUser;

public class SAPCESdk {

	private static final Log LOG = LogFactory.getLog(SAPCESdk.class);
	private static final IPDPHost LOCALHOST = new PDPHost(0x7F000001); // 127.0.0.1

	public List<CEEnforcement> checkResources(List<CERequest> requests,
			String additionalPQL, boolean ignoreBuiltinPolicies, int ipAddress,
			int timeout, boolean stopOnFirstDeny) throws CESdkTimeoutException,
			CESdkException {

		ArrayList<CEEnforcement> results = new ArrayList<CEEnforcement>();

		try {
			LinkedList<CheckableCallback> callbacks = new LinkedList<CheckableCallback>();

			for (CERequest request : requests) {
				// Join additional PQL into the already existing additional
				// attributes in request
				CENamedAttributes[] additionalAttrs = request
						.getAdditionalAttributes();

				int newLength = (request.getAdditionalAttributes() == null ? 0
						: request.getAdditionalAttributes().length)
						+ (additionalPQL != null ? 1 : 0);

				if (newLength != 0) {
					CENamedAttributes origAttrs[] = request
							.getAdditionalAttributes();
					additionalAttrs = new CENamedAttributes[newLength];

					if (origAttrs != null) {
						System.arraycopy(origAttrs, 0, additionalAttrs, 0,
								origAttrs.length);
					}

					if (additionalPQL != null) {
						CENamedAttributes policies = new CENamedAttributes(
								"policies");
						policies.add("pql", additionalPQL);
						policies.add("ignoredefault",
								ignoreBuiltinPolicies ? "yes" : "no");

						additionalAttrs[additionalAttrs.length - 1] = policies;
					}

				}

				CheckableCallback cb = new CheckableCallback();
				cb.setTrackingAttrs(request.getTrackingAttrs());
				callbacks.add(cb);

				checkResources(request.getAction(), request.getSource(),
						request.getSourceAttributes(), request.getDest(),
						request.getDestAttributes(), request.getUser(),
						request.getUserAttributes(), request.getApplication(),
						request.getApplicationAttributes(), additionalAttrs,
						request.getRecipients(), ipAddress,
						request.getPerformObligations(),
						request.getNoiseLevel(), timeout, cb);
			}

			/*
			 * There's no need to collect the results in the order they finish.
			 * We need all of them before the timeout happens, so why not just
			 * start at the beginning?
			 * 
			 * If we don't get all the response we give up. We do not return
			 * partial results.
			 */

			long waitTime = timeout;

			for (CheckableCallback cb : callbacks) {
				LOG.debug("Wait time is " + waitTime + " milliseconds");

				if (waitTime < 0) {
					throw new CESdkTimeoutException();
				}

				/*
				 * This time is not particularly precise, despite the name. On
				 * Windows it is usually some multiple of 16ms (with 0 as an
				 * option). It doesn't matter. If you have a 5s timeout then we
				 * don't guarantee that it will be *exactly* 5 seconds.
				 * 
				 * More accurate calls are available, but they are slower.
				 */
				long start = System.currentTimeMillis();
				try {
					CEEnforcement cee = cb.getResult(waitTime,
							TimeUnit.MILLISECONDS);
					results.add(cee);
					if (stopOnFirstDeny
							&& cee.getResponseAsString().equalsIgnoreCase(
									CEResponse.DENY.toString()))
						break;
				} catch (InterruptedException e) {
					throw new CESdkTimeoutException(e);
				}
				waitTime -= System.currentTimeMillis() - start;
			}
		} catch (CESdkException ee) {
			throw ee;
		}

		return results;
	}

	public CEEnforcement checkResources(String action, CEResource source,
			CEAttributes sourceAttrs, CEResource dest, CEAttributes destAttrs,
			CEUser user, CEAttributes userAttrs, CEApplication app,
			CEAttributes appAttrs, CENamedAttributes[] additionalAttrs,
			String[] recipients, int ipAddress, boolean performObligations,
			int noiseLevel, int timeout) throws CESdkTimeoutException,
			CESdkException {

		return checkResources(action, source, sourceAttrs, dest, destAttrs,
				user, userAttrs, app, appAttrs, additionalAttrs, recipients,
				ipAddress, performObligations, noiseLevel, timeout,
				IPDPSDKCallback.NONE);
	}

	private CEEnforcement checkResources(String action, CEResource source,
			CEAttributes sourceAttrs, CEResource dest, CEAttributes destAttrs,
			CEUser user, CEAttributes userAttrs, CEApplication app,
			CEAttributes appAttrs, CENamedAttributes[] additionalAttrs,
			String[] recipients, int ipAddress, boolean performObligations,
			int noiseLevel, int timeout, IPDPSDKCallback cb)
			throws CESdkTimeoutException, CESdkException {

		LOG.info("checkResources Server Impl called.");
		if (source == null)
			throw new CESdkException("CheckResources source can not be null");

		int resource_count = 1;
		if ((dest != null) && (!(dest.getName().length() == 0))) {
			resource_count++; // We have a target attribute
		}
		IPDPResource[] resources = new IPDPResource[resource_count];
		resources[0] = buildResource("from", source, sourceAttrs);
		if ((dest != null) && (!(dest.getName().length() == 0))) {
			resources[1] = buildResource("to", dest, destAttrs);
		}
		IPDPUser theUser = buildUser(user, userAttrs);
		IPDPNamedAttributes[] additionalData = buildAdditionalData(recipients,
				additionalAttrs);
		IPDPApplication application = buildApplication(app, appAttrs);
		IPDPHost host = buildHost(ipAddress);

		IPDPEnforcement ret = null;
		try {
			ret = PDPSDK.PDPQueryDecisionEngine(action, resources, theUser,
					application, host, performObligations, additionalData,
					noiseLevel, timeout, cb);
		} catch (IllegalArgumentException e) {
			throw new CESdkException("PDP query has illegal arguments", e);
		} catch (PDPTimeout e) {
			throw new CESdkTimeoutException("PDP query timedout");
		} catch (PDPException e) {
			throw new CESdkException("PDP query exception");
		}

		// The return value will be null if we have a callback, otherwise it
		// won't
		if (ret != null) {
			return new CEEnforcement(ret.getResult(), new CEAttributes(
					ret.getObligations()));
		}

		return null;
	}

	public void logObligationData(String logIdentifier, String assistantName,
			CEAttributes attributes) throws CESdkTimeoutException,
			CESdkException {
		LOG.info("SAPCESdk.LogObligationData called.");
		String assistantOptions = null;
		String assistantDescription = null;
		String assistantUserActions = null;
		int keyCounter=1;
		for(CEAttribute attribute:attributes.getAttributes())
		{
			if(attribute!=null&& attribute.getKey()!=null&&attribute.getValue()!=null)
			{
			assistantOptions=Integer.toString(keyCounter++);
			assistantDescription=attribute.getKey();
			assistantUserActions=attribute.getValue();
			PDPSDK.PDPLogObligationData(logIdentifier, assistantName,
					assistantOptions, assistantDescription, assistantUserActions);
			
			}
		}
		
		/*
		 * if (attributes.size() >= 1) { assistantOptions =
		 * attributes.getAttributes().get(0).getValue(); if (attributes.size()
		 * >= 2) { assistantDescription = attributes.getAttributes().get(1)
		 * .getValue(); if (attributes.size() >= 3) assistantUserActions =
		 * attributes.getAttributes().get(2) .getValue(); } }
		 */

		

	}

	private IPDPResource buildResource(final String dimensionName,
			final CEResource resource, final CEAttributes resourceAttrs) {
		IPDPResource theResource = new PDPResource(dimensionName,
				resource.getName(), resource.getType());

		if (resourceAttrs != null) {
			for (CEAttribute attr : resourceAttrs.getAttributes()) {
				theResource.setAttribute(attr.getKey().toLowerCase(),
						attr.getValue());
			}
		}
		return theResource;
	}

	private IPDPUser buildUser(final CEUser user, final CEAttributes userAttrs) {
		IPDPUser theUser = new PDPUser(user.getId(), user.getName());

		if (userAttrs != null) {
			for (CEAttribute attr : userAttrs.getAttributes()) {
				theUser.setAttribute(attr.getKey(), attr.getValue());
			}
		}

		return theUser;
	}

	private IPDPApplication buildApplication(final CEApplication app,
			final CEAttributes appAttrs) {
		IPDPApplication application = PDPApplication.NONE;

		if (!(app.getName().length() == 0)) {
			// TODO: do we need an actual PID here?
			application = new PDPApplication(app.getName(), 0L /* PID */);
			if (appAttrs != null) {
				for (CEAttribute attr : appAttrs.getAttributes()) {
					application.setAttribute(attr.getKey(), attr.getValue());
				}
			}
		}

		return application;
	}

	private IPDPHost buildHost(final int ipAddress) {
		IPDPHost host = LOCALHOST;

		if (ipAddress != 0) {
			host = new PDPHost(ipAddress);
		}

		return host;
	}

	private IPDPNamedAttributes[] buildAdditionalData(String[] recipients,
			CENamedAttributes[] additionalAttrs) {
		int additionalDataLength = 0;

		if (recipients != null && recipients.length > 0) {
			additionalDataLength++;
		}

		if (additionalAttrs != null) {
			additionalDataLength += additionalAttrs.length;
		}

		IPDPNamedAttributes[] additionalData = null;

		if (additionalDataLength > 0) {
			additionalData = new IPDPNamedAttributes[additionalDataLength];

			int index = 0;

			if (additionalAttrs != null) {
				for (CENamedAttributes namedAttrs : additionalAttrs) {
					additionalData[index] = new PDPNamedAttributes(
							namedAttrs.getName());
					for (CEAttribute attr : namedAttrs.getAttributes()) {
						additionalData[index].setAttribute(attr.getKey(),
								attr.getValue());
					}
					index++;
				}
			}
			if (recipients != null && recipients.length > 0) {
				additionalData[index] = new PDPNamedAttributes("sendto");
				for (String recipient : recipients) {
					additionalData[index].setAttribute("email", recipient);
				}
			}
		}

		return additionalData;
	}

	/**
	 * This class allows us to wait on the result of each individual callback.
	 * It would seem like Future<T> would do this, but the current
	 * implementations create a thread to run the action. We put a work unit on
	 * a queue to be handled by a thread pool, with the result returned via
	 * callback.
	 */
	private static class CheckableCallback implements IPDPSDKCallback {
		private Map<String, Object> trackingAttrs;
		ArrayBlockingQueue<CEEnforcement> q = new ArrayBlockingQueue<CEEnforcement>(
				1);

		public void callback(IPDPEnforcement result) {
			CEEnforcement cee = new CEEnforcement(result.getResult(),
					new CEAttributes(result.getObligations()));
			cee.setTrackingAttrs(this.trackingAttrs);
			q.add(cee);
		}

		public boolean hasResult() {
			return q.peek() != null;
		}

		public CEEnforcement getResult() throws InterruptedException {
			return q.take();
		}

		public CEEnforcement getResult(long timeout, TimeUnit unit)
				throws InterruptedException {
			return q.poll(timeout, unit);
		}

		public void setTrackingAttrs(Map map) {
			this.trackingAttrs = map;
		}

		public Map getTrackingAttrs() {
			return this.trackingAttrs;
		}
	}

}
