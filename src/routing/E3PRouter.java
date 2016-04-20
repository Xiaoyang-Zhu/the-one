package routing;

import static core.Constants.DEBUG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public static final int DEFAULT_PRED_ACCURACY = 4;
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
	
	/** COMMUNITIES_ATTRIBUTES: communityID and the number of nodes in each community */
	public static final String	COMMUNITIES_ATTRIBUTES = "communitiesAttributes";
	
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
	private int g = 0;
	/** indicator of maximum value - default value is true */
	private boolean ismax = false;
	/** variant to control the number of loops in leader nodes */
	private int j = 0;
	/** Leader indicator */
	private boolean isleader = false;

	
	/** Leaders' ID predefined in configuration file */
	private static String[]	leaders_id;
	/** Communities attributes string */
	private static String[]	communities_attributes_str;
	/** Communities attributes */
	private static String[]	communities_attributes;
	
	/** K value which is a constant such that 2 <= k < n, where n = |C|" */
	private static int k_value;
	private int encountered_nodes_num = 0;

	private String community_id;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	
	/** public delivery predictabilities */
	private Map<DTNHost, Double> r_intermediate_preds, r_init, r_final = null;
	
	/** public delivery predictabilities */
	private Map<DTNHost, Double> pub_preds;
	
	/** calculating  predictabilities temporary memory*/
	private Map<DTNHost, Double> cal_preds = null;
	
	/** temp sotrage for distributed value from other nodes */
	private Map<DTNHost, Double> tmp_preds = null;

	
	/** DTN leader Host*/
	private DTNHost leaderDTNHost = null;
	
	/** Integer */
	private static Map<String, Integer> communities_attrib;
	
	/** Number of distributed response to the leader */
	private Set <DTNHost> num_distrib_response;


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
		if (s.contains(COMMUNITIES_ATTRIBUTES)){
			int leaderNO = 0;
			communities_attrib = new HashMap<String, Integer>();
			communities_attributes_str = s.getSetting(COMMUNITIES_ATTRIBUTES).split(",");
			for (int i = 0; i < communities_attributes_str.length; i++) {
				communities_attributes = communities_attributes_str[i].split(":");
				communities_attrib.put(communities_attributes[0], E3PSettings.getInt(communities_attributes[1]));
				leaders_id[i] = communities_attributes[0] + leaderNO;
				leaderNO += E3PSettings.getInt(communities_attributes[1]);
			}
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
		this.isleader = r.isleader;
		this.g = r.g;
		this.j = r.j;
		this.ismax = r.ismax;
		this.encountered_nodes_num = r.encountered_nodes_num;
		this.community_id = r.community_id;
		this.leaderDTNHost = r.leaderDTNHost;
		//If the node is the leader of the community
		for (String s: leaders_id) {
			if (r.getHost().toString().startsWith(s)) {
				this.isleader = true;
			}
		}
		
		initPreds();

	}

	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
		
		/**Initiate the public delivery predictabilities */
//		this.r_intermediate_preds =new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
			
			//If the node is leader, then try to initiate the protocol
			if (isleader) {
				initiateE3PR();
			}
			
			if (cal_preds != null) {
				blurringOrignalProbability(con);
			}
			
		}
	}
	
	
	private void blurringOrignalProbability(Connection con) {
		encountered_nodes_num += 1;
		if (encountered_nodes_num == k_value + 1) {
			encountered_nodes_num = 0;
			//send the blurring probability to leader, if getHost() is leader hold, if not send it
			String id = SimClock.getIntTime() + "-" + 
					getHost().getAddress();
			Message res = new Message(this.getHost(),leaderDTNHost,
					id, 2048);
			res.addProperty("calculatingPreds", cal_preds);
			res.setAppID("RESPONSE_DISTRIB_PREDS");
			this.createNewMessage(res);
			if (isleader) {
				r_intermediate_preds = cal_preds;
			}
			cal_preds = null;

			
		} else {
			//send the random number to each other and calculate the cal_preds
			exchangeRandomNumbers(con, 2048);
		}
		
	}
	
	
	private Connection exchangeRandomNumbers(Connection conn, int resSize) {
		
		String id = SimClock.getIntTime() + "-" + 
				getHost().getAddress();
		Message randmsg = new Message(getHost(),conn.getOtherNode(getHost()),
				id, 1024);
		randmsg.setAppID("RANDOM_NUMBER_EXCHANGE_SIGNAL");
		
		// Generate and then set the random number
		Random rand = new Random();
		int random_val = rand.nextInt(10);
		randmsg.addProperty("random_value", random_val);
		randmsg.setResponseSize(resSize);

		this.createNewMessage(randmsg);

		//send the message to the other sides
		
/*		if (startTransfer(randmsg, conn) != RCV_OK) {
			return null;
		}
*/		
		return conn;
		
	}
	


	@Override
	protected void transferDone(Connection con) { 
		
		Message m = con.getMessage();

		if (m == null) {
			if (DEBUG) core.Debug.p("Null message for con " + con);
			return;
		}
		
		for (Map.Entry<DTNHost, Double> e : cal_preds.entrySet()) {

			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			int randval = (int)m.getProperty("random_value");
			double pNew = pOld - randval;
			cal_preds.put(e.getKey(), pNew);
		}
		
		/* was the message delivered to the final recipient? */
		if (m.getTo() == con.getOtherNode(getHost())) {
			this.deleteMessage(m.getId(), false);
		}
		
	}

	
	/**
	 * Updates public predictions for a community.
	 */
	private void updatePublicPreds() {
		

		
	}
	
	
	/**
	 * Initiates 3PR to flood the message carrying the initiating the 
	 * 3PR signal
	 */
	private boolean initiateE3PR() {
		
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate);
		//how many seconds
		if (timeDiff < 600) {
			return false;
		} 
		
		return this.createNewMessage(encapsulateInitSignal());

/*		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}
		
		List<Message> messages = new ArrayList<Message>();
		
		//Encapsulate the signal message and add to the message list
		messages.add(encapsulateInitSignal());
		this.sortByQueueMode(messages);
			
		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			
			assert community_id != null : "No community ID!";
			if (!(con.getOtherNode(getHost()).toString().startsWith(community_id))) {
				Message started = tryAllMessages(con, messages);
				if (started != null) {
					return con;
				}
			}

		}
		return null;
*/		
	}
	
	private Message encapsulateInitSignal() {
		/* Prepare the parameters for initiating signal */
		DTNHost dtnhost = getHost();
		g = SimClock.getIntTime();
		// Get the groupID of the community 
		String[] hostname= dtnhost.toString().split(String.valueOf
				(getHost().getAddress()));
		community_id = hostname[0];
		
		Message initmsg = new Message(getHost(),getHost(),
				dtnhost.toString() + g, 1024);
		initmsg.setAppID("INIT_SIGNAL");
		initmsg.addProperty("leaderDTNHost", dtnhost);
		initmsg.addProperty("maxInstanceID", g);
		initmsg.addProperty("communityID", community_id);
		initmsg.addProperty("sumInstanceID", j + g);
		
		return initmsg;
		
	}

		
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		/* check if msg is initiating signal message, then according to j
		 * to provide different p_i to calculate the value
		 */
		
		if (m.getAppID().equals("INIT_SIGNAL")) {
			int j = (int)m.getProperty("sumInstanceID") 
					- (int)m.getProperty("maxInstanceID");
			// assignment the value p_i
			if (j == 0) {
				cal_preds = this.getDeliveryPreds();
			} else if (j == 1) {

			} else if ((j > 2) && (j < pred_accuracy + 1)) {
				cal_preds = this.getDeliveryPreds();
				for (Map.Entry<DTNHost, Double> e : cal_preds.entrySet()) {

					double pOld = getPredFor(e.getKey()); // P(a,c)_old
					double pNew = Math.floor((pOld * Math.pow(2, j))) 
							- 2 * Math.floor((pOld * Math.pow(2, j-1)));
					cal_preds.put(e.getKey(), pNew);
				}

			} else if (j == pred_accuracy + 1) {
				cal_preds = this.getDeliveryPreds();
				// if ismax = true set 0
				
			} else {
				return null;
			}
			
			leaderDTNHost = (DTNHost)m.getProperty("leaderDTNHost");

		}  else if (m.getAppID().equals("RANDOM_NUMBER_EXCHANGE_SIGNAL")) {

		/* if the message is the RANDOM_NUMBER_EXCHANGE_SIGNAL message */
			
			for (Map.Entry<DTNHost, Double> e : cal_preds.entrySet()) {

				double pOld = getPredFor(e.getKey());
				int randval = (int)m.getProperty("random_value");
				double pNew = pOld + randval;
				cal_preds.put(e.getKey(), pNew);
			}
			if (m.getResponseSize() != 0) {
				Message res = new Message(this.getHost(),m.getFrom(),
						RESPONSE_PREFIX+m.getId(), m.getResponseSize());
				
				res.setAppID("RANDOM_NUMBER_EXCHANGE_SIGNAL");
				
				// Generate and then set the random number
				Random rand = new Random();
				int random_val = rand.nextInt(10);
				res.addProperty("random_value", random_val);
				this.createNewMessage(res);
			}
			
			
			this.deleteMessage(m.getId(), false);
			
		}  else if (m.getAppID().equals("RESPONSE_DISTRIB_PREDS") && isleader) {
			//Add to the public preds and add the new destinations and add the preds
			num_distrib_response = new HashSet<DTNHost>();
			num_distrib_response.add(m.getFrom());
			tmp_preds = (Map<DTNHost, Double>)m.getProperty("calculatingPreds");
			for (Map.Entry<DTNHost, Double> e : tmp_preds.entrySet()) {
				if (r_intermediate_preds.containsKey(e.getKey())) {
					r_intermediate_preds.replace(e.getKey(), r_intermediate_preds.get(e.getKey()) + e.getValue());
				} else {
					r_intermediate_preds.put(e.getKey(), e.getValue());
				}
			}
			
			/* if all distrib values are gathered, then flood the result to all nodes 
			in the same community carrying the kill message */
			if (num_distrib_response.size() == communities_attrib.get(community_id)) {
				String msgid = SimClock.getIntTime() + "-" + 
						getHost().getAddress();
				Message res = new Message(this.getHost(), getHost(),
						msgid, 1024);
				res.addProperty("intermediatePreds", r_intermediate_preds);
				res.setAppID("RESPONSE_SUM_PREDS");
				res.addProperty("j_value", j);
				this.createNewMessage(res);
			}
			
		}  else if (m.getAppID().equals("RESPONSE_SUM_PREDS")) {
			r_intermediate_preds = (Map<DTNHost, Double>)m.getProperty("intermediatePreds");
			if (m.getProperty("j_value").equals(0)) {
				r_init = r_intermediate_preds;
			} else if (m.getProperty("j_value").equals(pred_accuracy +1)) {
				r_final = r_intermediate_preds;
			}
			
			if (r_final != null) {
				for (Map.Entry<DTNHost, Double> e : r_final.entrySet()) {
					assert r_init.containsKey(e.getKey()) != false : "r_init and r_final not match!";
					r_init.replace(e.getKey(), r_init.get(e.getKey()) - e.getValue());
					pub_preds = r_init;
				}
			}
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
