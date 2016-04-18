package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import routing.util.RoutingInfo;
import util.Tuple;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

import java.util.Random;  


/**
 * Implementation of E3PR router as described in
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class E3PRouter extends ActiveRouter {
	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** the number of destination node default value */
	public static final int DEFAULT_DESTINATION_NUM = 4;
	/** the number of destination node default value */
	public static final int DEFAULT_PRED_ACCURACY = 3;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	/** k constant value */
	public static final int DEFAULT_K_VALUE = 2;
	
	/** E3P router's setting namespace ({@value})*/
	public static final String E3P_NS = "E3PRouter";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
	
	/** Leaders' ID predefined in configuration file */
	public static final String	LEADERS_ID = "leadersID";
	
	/** K value which is a constant such that 2 <= k < n, where n = |C|" */
	public static final String	K_VALUE = "kValue";

	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";
	
	/**
	 * The number of destination node (DESTINATION_NUM) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_DESTINATION_NUM}.
	 */
	public static final String DESTINATION_NUM = "destination_num";
	
	/**
	 * The number of accuracy -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_ACCURACY_PRED}.
	 */
	public static final String PRED_ACCURACY = "pred_accuracy";
	
	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;
	/** value of destination_num setting */
	private int destination_num;
	/** predictability accuracy */
	private int pred_accuracy;
	/** identifier 3PR instance */
	private static int g = 0;
	/** indicator of maximum value - default value is true */
	private static boolean ismax = true;
	/** variant to control the number of loops in leader nodes */
	private static int j = 0;
	
	/** Leaders' ID predefined in configuration file */
	private static String[]	leaders_id;
	
	/** K value which is a constant such that 2 <= k < n, where n = |C|" */
	private static int k_value;


	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	
	/** public delivery predictabilities */
	private Map<DTNHost, Double> pub_preds;
	
	/** calculating  predictabilities temporary memory*/
	private Map<DTNHost, Double> cal_preds;

	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public E3PRouter(Settings s) {
		super(s);
		Settings E3PSettings = new Settings(E3P_NS);
		secondsInTimeUnit = E3PSettings.getInt(SECONDS_IN_UNIT_S);
		if (E3PSettings.contains(BETA_S)) {
			beta = E3PSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}
		
		/* Add the number of destination nodes */
		if (E3PSettings.contains(DESTINATION_NUM)) {
			destination_num = E3PSettings.getInt(DESTINATION_NUM);
		}
		else {
			destination_num = DEFAULT_DESTINATION_NUM;
		}
		
		/* Predictability Accuracy*/
		if (E3PSettings.contains(PRED_ACCURACY)) {
			pred_accuracy = E3PSettings.getInt(PRED_ACCURACY);
		}
		else {
			pred_accuracy = DEFAULT_PRED_ACCURACY;
		}
		
		/* k_value */
		if (E3PSettings.contains(K_VALUE)) {
			k_value = E3PSettings.getInt(K_VALUE);
		}
		else {
			k_value = DEFAULT_K_VALUE;
		}
		
		/* Designate leaders of each community */
		if (s.contains(LEADERS_ID)){
			leaders_id = s.getSetting(LEADERS_ID).split(",");
		}

		initPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected E3PRouter(E3PRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		this.destination_num = r.destination_num;
		this.pred_accuracy = r.pred_accuracy;
		initPreds();
	}

	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
		
		/**Initiate the public delivery predictabilities */
		this.pub_preds =new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
			
			//If the node is leader, then try to updatePublicPreds
			for (String leader : leaders_id) {
				if (getHost().toString() == leader) {
					updatePublicPreds();
				}
			}
		}
	}
	
	
	
	/**
	 * Updates public predictions for a community.
	 */
	private void updatePublicPreds() {
		
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate);

		if (timeDiff < 30) {
			return;
		} else {
			initiateE3PR();
			calculatePrivateMax();				
		}
		
		this.lastAgeUpdate = SimClock.getTime();
		
	}
	
	
	/**
	 * Initiates 3PR to flood the message carrying the initiating the 
	 * 3PR signal
	 */
	private void initiateE3PR() {
		
		
		if (j == 0) {
			
			//p value
			
		} else if (j == 1) {
			
		} else if ((j > 1) && (j < pred_accuracy * 4 + 1)){
			
		} else if (j == pred_accuracy * 4 + 1) {
			
		} else {
			
		}
		//Encapsulate the signal message
		
		encapsulateInitSignal();
		
		//flood the message
		
	}
	
	private Message encapsulateInitSignal() {
		/* Prepare the parameters for initiating signal */
		String l = getHost().toString();
		g = SimClock.getIntTime();
		// Get the groupID of the community 
		String[] hostname= l.split(String.valueOf
				(getHost().getAddress()));
		
		Message initmsg = new Message(getHost(),getHost(),
				l + g, 1024);
		initmsg.addProperty("hostname", l);
		initmsg.addProperty("maxInstanceID", g);
		initmsg.addProperty("communityID", hostname[0]);
		initmsg.addProperty("sumInstanceID", j + g);
		
		//if they belong to the same community
		
		tryAllMessagesToAllConnections();
		
		return initmsg;
		
	}


	private void calculatePrivateMax() {
		
		List<Connection> connections = getConnections();

		g = SimClock.getIntTime();
		
		// loop for each private sum value
		for (int j = 0; j < pred_accuracy * 4 + 2; j++) {
			int h = g +j;
				
			if (j == 0) {
				floodingMessagesToAllConnections(connections, "MAXINIT", 
						h, 2048);
				
			} else if (j == 1) {
				
			} else if ((j > 1) && (j < pred_accuracy * 4 + 1)){
				
			} else if (j == pred_accuracy * 4 + 1) {
				
			} else {
				
			}
			
			floodingMessagesToAllConnections(connections, "MAXROUND", 
					h, 2048);
		}
		
		calculatePrivateSum();

	}
	
	private void floodingMessagesToAllConnections(List<Connection> connections, 
			String id_prefix, int instance_id, int resSize) {
		
		for (Connection conn : connections) {
			String id = id_prefix + SimClock.getIntTime() + "-" + 
					getHost().getAddress();
			Message initmsg = new Message(getHost(),conn.getOtherNode(getHost()),
					id, 1024);
			initmsg.addProperty("type", id_prefix);
			initmsg.addProperty("leader", getHost());
			initmsg.addProperty("g", g);
			if (instance_id != 0) {
				initmsg.addProperty("h", instance_id);
			}
			Random rand = new Random();
			int random_val = rand.nextInt(10);
			initmsg.addProperty("random_value", random_val);
			
			cal_preds = this.getDeliveryPreds();

			for (Map.Entry<DTNHost, Double> e : cal_preds.entrySet()) {

				double pOld = getPredFor(e.getKey()); // P(a,c)_old
				double pNew = pOld - random_val;
				cal_preds.put(e.getKey(), pNew);
			}
			
			getProcessedPredForFloodingMessage(instance_id - g, connections.size());
			this.createNewMessage(initmsg);
			initmsg.setResponseSize(resSize);
		}
		
	}
	
	private void getProcessedPredForFloodingMessage(int looptime_j, 
			int connections_num) {
		
		if (looptime_j == 0) {
			// messages all p_i
			for (int i = 0; i < connections_num; i++) {
				 Random rand = new Random();
				 
				 Map<DTNHost, Double> Preds =
							this.getDeliveryPreds();

					for (Map.Entry<DTNHost, Double> e : Preds.entrySet()) {

						double pOld = getPredFor(e.getKey()); // P(a,c)_old
						double pNew = pOld - rand.nextInt(10);
						preds.put(e.getKey(), pNew);
					}
					

				 this.getDeliveryPreds();
			}
	
		} else if (looptime_j == 1) {
			
		} else if ((looptime_j > 1) && (looptime_j < pred_accuracy * 4 + 1)){
			
		} else if (looptime_j == pred_accuracy * 4 + 1) {
			
		} else {
			
		}
		
	}
	
	private void calculatePrivateSum() {
		
	}
		
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		// check if msg MAXINIT or MAXROUND for this host
		if (m.getTo() == getHost() && m.getAppID() == "MAXINIT_SIG") {
			for (int i = 0; i < preds.size(); i++) {
				
			}
			
		} else if (m.getAppID() == "MAXROUND_SIG") {
			int j = (int)m.getProperty("h") - (int)m.getProperty("g");
			if (j == 0) {
				
			} else if (j == 1) {
				
			} else if ((j > 1) && (j < pred_accuracy * 4 + 1)){
				
			} else if (j == pred_accuracy * 4 + 1) {
				
			} else {
				return null;
			}
		}


		
		// check if msg was for this host and a response was requested
		if (m.getTo() == getHost() && m.getResponseSize() > 0) {
			// generate a response message
			Message res = new Message(this.getHost(),m.getFrom(),
					RESPONSE_PREFIX+m.getId(), m.getResponseSize());
			if (m.getAppID() == "MAXINIT_SIG") {
				res.addProperty("type", "MAXINIT_RESPONSE");
				res.setAppID("MAXINIT_SIG_RESPONSE");		
			}
			this.createNewMessage(res);
			this.getMessage(RESPONSE_PREFIX+m.getId()).setRequest(m);
		}

		return m;
	}


	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
	}

	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}
	
	
	public double getPubPredFor(DTNHost host) {
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}

	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof E3PRouter : "PRoPHET only works " +
			" with other routers of same type";

		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds =
			((E3PRouter)otherRouter).getDeliveryPreds();

		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}

			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) /
			secondsInTimeUnit;

		if (timeDiff == 0) {
			return;
		}

		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}

	
	/**
	 * Messages Encapsulation
	 * @param m the message needed to be encapsulated to protect privacy of
	 * source node and destination node
	 * @return the encapsulated message
	 */
	
	private class EncapMsg extends Message {
		
		public EncapMsg(DTNHost from, DTNHost to, String id, int size) {
			super(from, to, id, size);
		}
			
		protected Message setTo(Message m, DTNHost to){
			//change the destination to pass the examination in start transfer
			EncapMsg encap_m = new EncapMsg(m.getFrom(), 
					to, m.getId(),  m.getSize());
			encap_m.copyFrom(m);
			return encap_m;
		}
		
		protected EncapMsg msg_replicate_encap(Message m) {
			DTNHost orig_from = m.getFrom();
			DTNHost orig_to = m.getTo();
			
			
			String encrypt_node_str = encrypt_source_node(orig_from);
			
			List<DTNHost> dtnhost_destinations_list = 
					gen_pseu_list_destination_node(orig_to);
			String destination_list = dtnhost_destinations_list.toString();
			
			EncapMsg encap_m = new EncapMsg(gen_pseu_dtnhost_id(orig_from), 
					gen_pseu_dtnhost_id(orig_to), m.getId(),  m.getSize() + 
					destination_list.length() + encrypt_node_str.length());
			
			encap_m.copyFrom(m);
			
			encap_m.addProperty("source_pseudo", encrypt_node_str);
			encap_m.addProperty("destinations_list", dtnhost_destinations_list);
			
			return encap_m;
		}
	}
	
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryOtherMessages();
	}

	
	/**
	 * Override the exchangeDeliverableMessages to deliver messages to many
	 * destinations rather than only one.
	 */
	@Override
	protected Connection exchangeDeliverableMessages() {
		List<Connection> connections = getConnections();

		if (connections.size() == 0) {
			return null;
		}
		
		@SuppressWarnings(value = "unchecked")
		Tuple<Message, Connection> t =
			tryMessagesForConnected(sortByQueueMode(getMessagesForConnected()));

		if (t != null) {
			return t.getValue(); // started transfer
		}

		// didn't start transfer to any node -> ask messages from connected
		for (Connection con : connections) {
			if (con.getOtherNode(getHost()).requestDeliverableMessages(con)) {
				return con;
			}
		}

		return null;	
	}
	
	
	@Override
	protected List<Tuple<Message, Connection>> getMessagesForConnected() {
		if (getNrofMessages() == 0 || getConnections().size() == 0) {
			/* no messages -> empty list */
			return new ArrayList<Tuple<Message, Connection>>(0);
		}

		List<Tuple<Message, Connection>> forTuples =
			new ArrayList<Tuple<Message, Connection>>();
		for (Message m : getMessageCollection()) {
			
			/* Get the message originated from this node and encapsulate it */
			
			EncapMsg encap_msg = new EncapMsg(m.getFrom(), m.getTo(), m.getId(), m.getSize());
			Message msg = new Message(m.getFrom(), m.getTo(), m.getId(), m.getSize());
			if( m.getProperty("destinations_list") == null){
				msg = encap_msg.msg_replicate_encap(m);
			} else {
				msg = m.replicate();
			}
			
			for (Connection con : getConnections()) {
				DTNHost to = con.getOtherNode(getHost());
				
				/*
				 * Obtain the list of destination to match the connected node 
				 * If there is one node who is one destination node in the
				 * list, the message will be picked up
				 */
				
				@SuppressWarnings("unchecked")
				List <DTNHost> dtnhost_destinations_list 
					= (List <DTNHost>) msg.getProperty("destinations_list");
				if (dtnhost_destinations_list.equals(to)) {
					forTuples.add(new Tuple<Message, Connection>(encap_msg.setTo(msg, to),con));
				}
			}	

		}

		return forTuples;
	}
	
	@Override
	public boolean requestDeliverableMessages(Connection con) {
		if (isTransferring()) {
			return false;
		}

		DTNHost other = con.getOtherNode(getHost());
		/* do a copy to avoid concurrent modification exceptions
		 * (startTransfer may remove messages) */
		ArrayList<Message> temp =
			new ArrayList<Message>(this.getMessageCollection());
		for (Message m : temp) {
			
			EncapMsg encap_msg = new EncapMsg(m.getFrom(), m.getTo(), m.getId(), m.getSize());
			Message msg = new Message(m.getFrom(), m.getTo(), m.getId(), m.getSize());
			
			if( m.getProperty("destinations_list") == null){
				msg = encap_msg.msg_replicate_encap(m);
			} else {
				msg = m.replicate();
			}
			
			@SuppressWarnings("unchecked")
			List <DTNHost> dtnhost_destinations_list 
				= (List <DTNHost>) msg.getProperty("destinations_list");
			if (dtnhost_destinations_list.equals(other)) {
				if (startTransfer(encap_msg.setTo(msg, other), con) == RCV_OK) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	private String encrypt_source_node(DTNHost from) {
		String str_dtnhost = from.toString();
		
		/*
		 * Add encryption code
		 */		
		
		return str_dtnhost;
	}
	
	private DTNHost gen_pseu_dtnhost_id(DTNHost host) {
		return host;
	}
	
	private List<DTNHost> gen_pseu_list_destination_node(DTNHost to) {
		List<DTNHost> dtnhost_list =
				new ArrayList<DTNHost>();
		
		for (int i = destination_num;i<1;i++) {
			dtnhost_list.add(to);
			
		}
		
		return dtnhost_list;
	} 
	
	
	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages =
			new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			E3PRouter othRouter = (E3PRouter)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
					// the other node has higher probability of delivery
					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}
		}

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}

	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator
		<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((E3PRouter)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((E3PRouter)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() +
				" delivery prediction(s)");

		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();

			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f",
					host, value)));
		}

		top.addMoreInfo(ri);
		return top;
	}

	@Override
	public MessageRouter replicate() {
		E3PRouter r = new E3PRouter(this);
		return r;
	}

}
