package org.hyperledger.fabric.samples.assettransfer;

import java.time.LocalTime;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
//import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
//import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
//import org.hyperledger.fabric.shim.ledger.KeyValue;
//import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.KeyValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import com.google.gson.Gson;

@Contract(
        name = "basic",
        info = @Info(
                title = "Energy certifier",
                description = "A simple energy certifier chaincode example",
                version = "0.0.1-SNAPSHOT"))
@Default
public final class EnergyCertifier implements ContractInterface {

    private final Gson gson = new Gson();


    /**
     * Create a new user
     * @param ctx the transaction context
     * @param ID the ID of the user
     * @param name the name of the user
     * @param surname the surname of the user
     * @param email the email of the user
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createUser(final Context ctx, final String ID, final String name, final String surname, final String email) {
        ChaincodeStub stub = ctx.getStub();

        if(userExists(ctx, ID)) {
            String errorMessage = String.format("User %s already exists", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        User user = new User(ID, name, surname, email);

        String userState = gson.toJson(user);
        stub.putStringState("user"+ID, userState);
    }


	/**
	 * Create a new group
	 * @param ctx the transaction context
	 * @param ID the ID of the group
	 * @param name the name of the group
	 * @param location the location of the group
	 * @param user the user that creates the group
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void createGroup(final Context ctx, final String ID, final String name, final String location, final String userID, final String proposalTimer, final String voteTimer) {
		ChaincodeStub stub = ctx.getStub();

		if(groupExists(ctx, ID)) {
			String errorMessage = String.format("Group %s already exists", ID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		User user = getUser(ctx, userID);
		Group group = new Group(ID, name, location, Integer.valueOf(proposalTimer), Integer.valueOf(voteTimer));

		//Check if user exists
		if(user == null) {
			String errorMessage = String.format("User %s does not exist", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		group.addUser(user.getID(), Role.ADMIN);
		user.getSubscription().add(group.getID());

		System.out.println("Group created " + group.getID());

		String groupState = gson.toJson(group);
		stub.putStringState("group" + ID, groupState);
		String userState = gson.toJson(user);
		stub.putStringState("user" + userID, userState); 
	}

	
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void createPolicy(final Context ctx, final String userID, final String ID, final String policyType, final String rules){
		PolicyType metricType = null;

		String rulesString = rules.substring(1, rules.length()-1);
		rulesString = rulesString.replace("\"", "");
		List<String> rulesList = Arrays.asList(rulesString.split(","));

		//Check if policy exists
		if(policyExists(ctx, ID)) {
			String errorMessage = String.format("Policy %s already exists", ID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		if(policyType.equals("TIME")){
			metricType = PolicyType.TIME;
		}
		else if(policyType.equals("VALUE")){
			metricType = PolicyType.VALUE;
		}
		else if(policyType.equals("TIMEVALUE")){
			metricType = PolicyType.TIMEVALUE;
		}
		else if (metricType == null) {
			String errorMessage = String.format("Metric %s is not valid", policyType);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
		
		User user = getUser(ctx, userID);
		if (user == null) {
			String errorMessage = String.format("User %s does not exist", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
		//genero l'ID a caso 
		
		System.out.println("Initialized, Creating policy " + ID);
		Policy policy = user.createPolicy(ID, metricType, rulesList);
		ChaincodeStub stub = ctx.getStub();
		System.out.println(policy.toString());
		String policyState = gson.toJson(policy);
		stub.putStringState("policy"+ID, policyState);
		String userState = gson.toJson(user);
		stub.putStringState("user"+userID, userState);

	}

    /**
     * Checks the existence of a user in the ledger
     * @param ctx the transaction context
     * @param ID the ID of the user
     * @return boolean indicating the existence of the user
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean userExists(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        String userState = stub.getStringState("user"+ID);

        return (userState != null && !userState.isEmpty());
    }

	/**
	 * Checks the existence of a policy in the ledger
	 * @param ctx the transaction context
	 * @param ID the ID of the policy
	 * @return boolean indicating the existence of the policy
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public boolean policyExists(final Context ctx, final String ID) {
		ChaincodeStub stub = ctx.getStub();
		String policyState = stub.getStringState("policy"+ID);

		return (policyState != null && !policyState.isEmpty());
	}

    /**
	 * Checks the existence of a group in the ledger
	 * @param ctx the transaction context
	 * @param ID the ID of the group
	 * @return boolean indicating the existence of the group
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public boolean groupExists(final Context ctx, final String ID) {
		ChaincodeStub stub = ctx.getStub();
		String groupState = stub.getStringState("group"+ID);

		return (groupState != null && !groupState.isEmpty());
	}

	/**
	 * Checks the existence of a device in the ledger
	 * @param ctx the transaction context
	 * @param ID the ID of the device
	 * @return boolean indicating the existence of the device
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public boolean deviceExists(final Context ctx, final String ID) {
		ChaincodeStub stub = ctx.getStub();
		String deviceState = stub.getStringState("device"+ID);

		return (deviceState != null && !deviceState.isEmpty());
	}

	/**
	 * Send a request to join a group
	 * @param ctx the transaction context
	 * @param groupID the ID of the group
	 * @param userID the ID of the user
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void joinGroup(final Context ctx, final String groupID, final String userID) {
		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);
		User user = getUser(ctx, userID);
		String adminID = group.getAdmin();
		User admin = getUser(ctx, adminID);

		//Check if user exists
		if(user == null) {
			String errorMessage = String.format("User %s does not exist", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if group has an admin
		if(admin == null) {
			String errorMessage = String.format("Group %s has no admin", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user is already in the group
		if(group.getMembers().containsKey(userID)) {
			String errorMessage = String.format("User %s is already in the group", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		group.addJoiningRequest(userID);

		String groupState = gson.toJson(group);
		stub.putStringState("group" + groupID, groupState);
	}

	/**
	 * Accept a request to join a group
	 * @param ctx the transaction context
	 * @param groupID the ID of the group
	 * @param userID the ID of the user
	 * @param adminID the ID of the admin
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void acceptRequest(final Context ctx, final String groupID, final String userID, final String adminID) {
		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);
		User user = getUser(ctx, userID);
		User admin = getUser(ctx, adminID);

		//Check if user exists
		if(user == null) {
			String errorMessage = String.format("User %s does not exist", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if admin exists
		if(admin == null) {
			String errorMessage = String.format("Admin %s does not exist", adminID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user has a request
		if(!group.getJoining_requests().contains(userID)) {
			String errorMessage = String.format("User %s has no request", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if admin is the admin of the group
		if(!group.getMembers().get(adminID).equals(Role.ADMIN)) {
			String errorMessage = String.format("User %s is not the admin of the group", adminID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		admin.acceptRequest(group, user);
		
		String groupState = gson.toJson(group);
		String userState = gson.toJson(user);
		stub.putStringState("group" + groupID, groupState);
		stub.putStringState("user" + userID, userState);
	}
	

	/**
	 * Register a new device
	 * @param ctx the transaction context
	 * @param ID the ID of the device
	 * @param unit the unit of the device
	 * @param user the user that register the device
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public String registerDevice(final Context ctx, final String ID, final String unit, final String userID, final String groupID) throws NoSuchAlgorithmException {
		ChaincodeStub stub = ctx.getStub();

		if(deviceExists(ctx, ID)) {
			String errorMessage = String.format("Device %s already exists", ID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		User user = getUser(ctx, userID);
		Device device = new Device(ID, unit, userID);
		user.addDevice(ID);

		String deviceState = gson.toJson(device);
		String userState = gson.toJson(user);
		stub.putStringState("device"+ID, deviceState);
		stub.putStringState("user"+userID, userState);

		IDToken idToken = new IDToken(groupID, userID, ID);
		String uuid = idToken.getId();

		String idTokenState = gson.toJson(idToken);
		stub.putStringState("UUID"+uuid, idTokenState);
		System.out.println("UUID generated: " + uuid);

		return uuid;
	}


	/**
	 * Starts the policy proposal phase 
	 * @param ctx the transaction context
	 * @param groupID the ID of the group
	 * @param adminID the ID of the admin
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void startPolicyProposalPhase(final Context ctx, final String groupID, final String adminID) {
		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);
		User admin = getUser(ctx, adminID);

		//Check if user exists
		if(admin == null) {
			String errorMessage = String.format("User %s does not exist", adminID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user is the admin of the group
		if(!group.getMembers().get(adminID).equals(Role.ADMIN)) {
			String errorMessage = String.format("User %s is not the admin of the group", adminID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if state is NEW
		if(!group.getState().equals(GroupState.NEW)) {
			String errorMessage = String.format("The group is not in new state");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		LocalTime time = LocalTime.now().withNano(0);
		group.setState(GroupState.POLICYPROPOSAL);
		group.setTimer(time);

		String groupState = gson.toJson(group);
		stub.putStringState("group"+groupID, groupState);
	}

	/**
	 * Send a policy proposal 
	 * @param ctx the transaction context
	 * @param groupID the ID of the group
	 * @param userID the ID of the user
	 * @param policyID the ID of the policy
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void sendProposal(final Context ctx, final String groupID, final String userID, final String policyID) {
		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);
		User user = getUser(ctx, userID);
		Policy policy = getPolicy(ctx, policyID);

		//Check if the group exists
		if(group == null) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user exists
		if(user == null) {
			String errorMessage = String.format("User %s does not exist", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user is in the group
		if(!group.getMembers().containsKey(userID)) {
			String errorMessage = String.format("User %s is not in the group", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the policy exists
		if(policy == null) {
			String errorMessage = String.format("Policy %s does not exist", policyID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		GroupState state = getGroupState(ctx, group);

		if (state == GroupState.POLICYPROPOSAL) {
			group.addProposal(user.getPolicy(policyID));
			String groupState = gson.toJson(group);
			stub.putStringState("group"+groupID, groupState);
		} else {
			String errorMessage = String.format("The group is not in policy proposal state");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
	}


	/**
	 * Vote for a policy 
	 * @param ctx the transaction context
	 * @param groupID the ID of the group
	 * @param userID the ID of the user
	 * @param policyID the ID of the policy
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void vote(final Context ctx, final String groupID, final String userID, final String policyID) {
		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);
		User user = getUser(ctx, userID);
		Policy policy = getPolicy(ctx, policyID);

		//Check if user exists
		if(user == null) {
			String errorMessage = String.format("User %s does not exist", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user is in the group
		if(!group.getMembers().containsKey(userID)) {
			String errorMessage = String.format("User %s is not in the group", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if there is a policy to vote
		if(group.getProposed_policies().isEmpty()) {
			String errorMessage = String.format("There are no policies to vote");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the policy exists
		if(policy == null) {
			String errorMessage = String.format("Policy %s does not exist", policyID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the policy is in the policies to vote
		if(!group.getProposed_policies().containsKey(policy)) {
			String errorMessage = String.format("Policy %s is not in the policies to vote", policyID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		GroupState state = getGroupState(ctx, group);
		Boolean hasVoted = user.getHasVoted();

		if(!hasVoted && state == GroupState.VOTEOPEN){
			user.vote(group, policy);
			String groupState = gson.toJson(group);
			stub.putStringState("group"+groupID, groupState);
		} else if (hasVoted == true) {
			String errorMessage = String.format("User %s has already voted", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		} else {
			String errorMessage = String.format("The group is not in vote open state");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
	}

	/**
	 * Send a measure
	 * @param ctx the transaction context without performing the checks 
	 * @param measureID the ID of the measure
	 * @param measureUUID the UUID of the measure
	 * @param values the values 
	 * @param valueNames the name of the values 
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void sendMeasureNoCheck(final Context ctx, final String measureID, final String measureUUID, final String values, final String valueNames){
		ChaincodeStub stub = ctx.getStub();

		String valueNamesString = valueNames.substring(1, valueNames.length()-1);
		valueNamesString = valueNamesString.replace("\"", "");
		List<String> valueNamesList = Arrays.asList(valueNamesString.split(","));

		String valuesString = values.substring(1, values.length()-1);
		valuesString = valuesString.replace("\"", "");
		List<String> valuesList = Arrays.asList(valuesString.split(","));
		//Cast values to float
		List<Float> valuesListFloat = new ArrayList<Float>();
		for (String value : valuesList) {
			valuesListFloat.add(Float.parseFloat(value));
		}
		//Get parameter from UUID JSON string
		IDToken UUIDToken = gson.fromJson(stub.getStringState("UUID"+measureUUID), IDToken.class);
		System.out.println("UUIDToken: " + UUIDToken);

		String userId = UUIDToken.getUserid();
		String groupID = UUIDToken.getGroupid();
		String deviceID = UUIDToken.getDeviceid();
		
		Group group = getGroup(ctx, groupID);
		User user = getUser(ctx, userId);

		//Check if group exists
		if(group == null) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the device exists
		if(!deviceExists(ctx, deviceID)) {
			String errorMessage = String.format("Device %s does not exist", deviceID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user is in the group
		if(!group.getMembers().containsKey(user.getID())) {
			String errorMessage = String.format("User %s is not in the group", user.getID());
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the user has the device
		if(!user.hasDevice(deviceID)) {
			String errorMessage = String.format("User %s has no device %s", user.getID(), deviceID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		GroupState state = getGroupState(ctx, group);

		if (state == GroupState.MONITORING) {
			LocalTime time = LocalTime.now().withNano(0);
			Measure measure = new Measure(user.getID(), measureID, time, valuesList, deviceID, groupID, valueNamesList);
			String measureState = gson.toJson(measure);
			stub.putStringState("measure"+measureID, measureState);
			//for (String data : valueNamesList){
			//	user.setLastMeasure(data, measure);
			//}
			String userState = gson.toJson(user);
			stub.putStringState("user"+userId, userState);
		} else {
			String errorMessage = String.format("The group is not in monitoring state");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

	}


	/**
	 * check if there is a violation 
	 * @param ctx the transaction context without performing the checks 
	 * @param measureID the ID of the measure
	 * @return a boolean 
	 */
	public Boolean hasViolation(final Context ctx, final String measureID) {
		ChaincodeStub stub = ctx.getStub();
		//Get all the violation that starts with "violation12AUTO"
		
		QueryResultsIterator<KeyValue> results = stub.getStateByRange("violation"+measureID, "violation"+measureID+"z");
		List<Violation> violations = new ArrayList<Violation>();

		for (KeyValue result : results) {
			String violationState = result.getStringValue();
			Violation violation = gson.fromJson(violationState, Violation.class);
			violations.add(violation);
		}

		return !violations.isEmpty();
	}


	/**
	 * get the last timing of the last violation 
	 * @param ctx the transaction context without performing the checks 
	 * @param measureID the ID of the measure
	 * @return the timing of the last violation 
	 */
	public LocalTime lastViolationTime (final Context ctx, final String measureID) {
		ChaincodeStub stub = ctx.getStub();
		//Get all the violation that starts with "violation12AUTO"
		
		QueryResultsIterator<KeyValue> results = stub.getStateByRange("violation"+measureID, "violation"+measureID+"z");
		List<Violation> violations = new ArrayList<Violation>();

		for (KeyValue result : results) {
			String violationState = result.getStringValue();
			Violation violation = gson.fromJson(violationState, Violation.class);
			violations.add(violation);
		}

		LocalTime lastViolationTime = null;
		for (Violation violation : violations) {
			if (lastViolationTime == null || violation.getTimestamp().isAfter(lastViolationTime)) {
				lastViolationTime = violation.getTimestamp();
			}
		}

		return lastViolationTime;
	}




	/**
	 * Remove all the measures, violations and reset the timer of the group
	 * @param ctx the transaction context without performing the checks 
	 * @param groupID the ID of the group
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void resetGroup(final Context ctx, final String groupID){
		ChaincodeStub stub = ctx.getStub();
		Group group = getGroup(ctx, groupID);

		//Check if group exists
		if(group == null) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the group is in monitoring state
		if(!group.getState().equals(GroupState.MONITORING)) {
			String errorMessage = String.format("The group is in monitoring state");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		group.resetMVT();

		String groupState = gson.toJson(group);
		stub.putStringState("group"+groupID, groupState);

		//WARN THIS WILL DELETE ALL THE MEASURES AND VIOLATIONS IN THE BLOCKCHAIN
		QueryResultsIterator<KeyValue> results = stub.getStateByRange("measure", "measurez");
		for (KeyValue result : results) {
			stub.delState(result.getKey());
		}

		QueryResultsIterator<KeyValue> results2 = stub.getStateByRange("violation", "violationz");
		for (KeyValue result : results2) {
			stub.delState(result.getKey());
		}

	}

	/**
	 * Set timer in the group
	 * @param ctx the transaction context without performing the checks 
	 * @param groupID the ID of the group
	 * @param timer the timer for the group
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void setTimer(final Context ctx, final String groupID, final String timer){
		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);

		//Check if group exists
		if(group == null) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		LocalTime time = LocalTime.parse(timer);
		group.setTimer(time);

		String groupState = gson.toJson(group);
		stub.putStringState("group"+groupID, groupState);
	}


	/**
	 * Get a user from the ledger
	 * @param ctx the transaction context
	 * @param ID the ID of the user
	 * @return the user
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public User getUser(final Context ctx, final String ID) {
		ChaincodeStub stub = ctx.getStub();

		String userState = stub.getStringState("user"+ID);
		if (userState == null || userState.isEmpty()) {
			String errorMessage = String.format("User %s does not exist", ID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
		System.out.println("User deserialized " + userState);

		return gson.fromJson(userState, User.class);
	}

	/**
	 * Get a group from the ledger
	 * @param ctx the transaction context
	 * @param ID the ID of the group
	 * @return the group
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Group getGroup(final Context ctx, final String ID) {
		ChaincodeStub stub = ctx.getStub();

		String groupState = stub.getStringState("group"+ID);
		if (groupState == null || groupState.isEmpty()) {
			String errorMessage = String.format("Group %s does not exist", ID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
		System.out.println("Group deserialized " + groupState);

		return gson.fromJson(groupState, Group.class); 
	}

	/**
	 * Get group's state from the ledger
	 * @param ctx the transaction context
	 * @param Group the group
	 * @return the GroupState
	 */
	public GroupState getGroupState(final Context ctx, Group group) {
		LocalTime time = LocalTime.now().withNano(0);
		LocalTime lastTick = group.getGroup_timestamp();
		Duration duration = Duration.between(lastTick, time);
		Boolean voteEnded = group.getVoteEnded();

		System.out.println("Duration: " + duration.toSeconds() + " seconds, VoteEnded: " + voteEnded);

		Integer proposalTimer = group.getProposal_duration();
		Integer voteTimer = group.getVoting_duration();
		Integer monitoringTimer = proposalTimer + voteTimer;

		if(duration.toSeconds() < proposalTimer && !voteEnded) {
			group.setState(GroupState.POLICYPROPOSAL);
			System.out.println("Setting state to policy proposal");
			return GroupState.POLICYPROPOSAL;
		} else if(duration.toSeconds() >= proposalTimer && duration.toSeconds() < monitoringTimer && !voteEnded) {
			group.setState(GroupState.VOTEOPEN);
			System.out.println("Setting state to vote open");
			return GroupState.VOTEOPEN;
		} else if(duration.toSeconds() >= monitoringTimer && !voteEnded) {
			group.setState(GroupState.MONITORING);
			System.out.println("Setting state to monitoring");
			String mostVotedPolicy = group.endPolicyVote();
			if (mostVotedPolicy != null) {
				Policy mostVoted = getPolicy(ctx, mostVotedPolicy);
				group.setPolicy(mostVoted);
			}
			group.resetWarnings();
			group.setWarning_reset_timestamp(time);
			ctx.getStub().putStringState("group"+group.getID(), gson.toJson(group));
			return GroupState.MONITORING;
		} else if (voteEnded) {
			System.out.println("Vote ended, setting state to monitoring");
			group.setState(GroupState.MONITORING);
			return GroupState.MONITORING;
		} else {
			System.out.println("Setting state to new");
			group.setState(GroupState.NEW);
			return GroupState.NEW;
		}
	}


	/**
	 * Get a specific measure from the blockchain
	 * @param ctx the transaction context
	 * @param measureID the ID of the measure
	 * @return the measure
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Measure getMeasure(final Context ctx, final String measureID){
		ChaincodeStub stub = ctx.getStub();

		String measureState = stub.getStringState("measure"+measureID);
		if (measureState == null || measureState.isEmpty()) {
			String errorMessage = String.format("Measure %s does not exist", measureID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
		System.out.println("Measure deserialized " + measureState);

		return gson.fromJson(measureState, Measure.class);
	}


	/**
	 * Get the policy from the blockchain
	 * @param ctx the transaction context
	 * @param ID the ID of the policy
	 * @return the policy
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Policy getPolicy(final Context ctx, final String ID) {
		ChaincodeStub stub = ctx.getStub();

		String policyState = stub.getStringState("policy"+ID);
		if (policyState == null || policyState.isEmpty()) {
			String errorMessage = String.format("Policy %s does not exist", ID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
		System.out.println("Policy deserialized " + policyState);

		return gson.fromJson(policyState, Policy.class);
	}


	/**
	 * Get the last measure from the blockchain
	 * @param ctx the transaction context
	 * @param groupID the ID of the group
	 * @param userID the ID of the user
	 * @return the last measure
	 */
	public Measure getLastMeasure(final Context ctx, final String groupID, final String userID) {
		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);
		User user = getUser(ctx, userID);

		//Check if group exists
		if(group == null) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user exists
		if(user == null) {
			String errorMessage = String.format("User %s does not exist", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user is in the group
		if(!group.getMembers().containsKey(userID)) {
			String errorMessage = String.format("User %s is not in the group", userID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("measure", "measurez");
		Measure measure = null;
		for (KeyValue result : results) {
			String measureState = result.getStringValue();
			Measure measureTemp = gson.fromJson(measureState, Measure.class);
			if(measureTemp.getUserID().equals(userID) && measureTemp.getGroupID().equals(groupID)){
				if(measure == null || measureTemp.getTimestamp().isAfter(measure.getTimestamp())) {
					measure = measureTemp;
				}
			}
		}

		return measure;
	}


	/**
	 * Get the violation from the blockchain
	 * @param ctx the transaction context
	 * @param violationID the ID of the violation
	 * @return the violation
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public String getViolation(final Context ctx, final String violationID) {
		ChaincodeStub stub = ctx.getStub();

		String violationState = stub.getStringState("violation"+violationID);
		if (violationState == null || violationState.isEmpty()) {
			String errorMessage = String.format("Violation %s does not exist", violationID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
		System.out.println("Violation deserialized " + violationState);

		return violationState;
	}

	
	/**
	 * Getting the violations of a group
	 * @param ctx the transaction context
	 * @param groupID the ID of the group
	 * @return violations of the group 
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public String getViolationsInGroup(final Context ctx, final String groupID) {
		ChaincodeStub stub = ctx.getStub();
		
		if (!groupExists(ctx, groupID)) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("violation", "violationz");
		List<Violation> violations = new ArrayList<Violation>();

		for (KeyValue result : results) {
			String violationState = result.getStringValue();
			Violation violation = gson.fromJson(violationState, Violation.class);
			violations.add(violation);
		}

		String violationsJson = gson.toJson(violations);
		return violationsJson;
	}


	/**
	 * Get all the measures on the blockchain
	 * @param ctx the transaction context
	 * @return all the measures
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public String getAllMeasures(final Context ctx){
		ChaincodeStub stub = ctx.getStub();

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("measure", "measurez");
		List<Measure> queryResults = new ArrayList<Measure>();

		for (KeyValue result : results) {
			String value = result.getStringValue();
			Measure measure = gson.fromJson(value, Measure.class);
			queryResults.add(measure);
		}

		String queryResultsJ = gson.toJson(queryResults);

		return queryResultsJ;
	}

	/**
	 * Get all the users on the blockchain
	 * @param ctx the transaction context
	 * @return all the users
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public String getAllUsers(final Context ctx){
		ChaincodeStub stub = ctx.getStub();

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("user", "userz");
		List<User> queryResults = new ArrayList<User>();

		for (KeyValue result : results) {
			String value = result.getStringValue();
			User user = gson.fromJson(value, User.class);
			queryResults.add(user);
		}

		String queryResultsJ = gson.toJson(queryResults);

		return queryResultsJ;
	}


	/**
	 * Get all the groups on the blockchain
	 * @param ctx the transaction context
	 * @return all the groups
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public String getAllGroups(final Context ctx){
		ChaincodeStub stub = ctx.getStub();

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("group", "groupz");
		List<Group> queryResults = new ArrayList<Group>();

		for (KeyValue result : results) {
			String value = result.getStringValue();
			Group group = gson.fromJson(value, Group.class);
			queryResults.add(group);
		}

		String queryResultsJ = gson.toJson(queryResults);

		return queryResultsJ;
	}


	/**
	 * Get all the violations on the blockchain
	 * @param ctx the transaction context
	 * @return all the violations
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public String getAllViolations(final Context ctx){
		ChaincodeStub stub = ctx.getStub();

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("violation", "violationz");
		List<Violation> queryResults = new ArrayList<Violation>();

		for (KeyValue result : results) {
			String value = result.getStringValue();
			Violation violation = gson.fromJson(value, Violation.class);
			queryResults.add(violation);
		}

		String queryResultsJ = gson.toJson(queryResults);

		return queryResultsJ;
	}


	/**
	 * Get all the blockchain
	 * @param ctx the transaction context
	 * @return all the blockchain
	 */
	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public String getAll(final Context ctx){
		ChaincodeStub stub = ctx.getStub();

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");
		List<String> queryResults = new ArrayList<String>();

		for (KeyValue result : results) {
			String value = result.getStringValue();
			queryResults.add(value);
		}

		String queryResultsJ = gson.toJson(queryResults);

		return queryResultsJ;
	}



	private Pair<Boolean, Violation> check_OneOpeatorValueRule(String measureID, boolean hasViolated, String operator, Float threshold, Float value_measured, Integer user_violation, Integer tolerance, Group group, String rule_id, String user_id, String error, LocalTime time, String deviceID){
		Violation violation = new Violation();
		switch (operator){ 
			case ">":
				if (value_measured <= threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);

					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						hasViolated = true;
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time,String.valueOf(value_measured), error);
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			case ">=":
				if (value_measured < threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						hasViolated = true;
						//group.setUsersWarnings(user_id, rule_id, 0);

					}
				}
				break;
			case "<":
				if (value_measured >= threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){		
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						hasViolated = true;
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			case "<=":
				if (value_measured >= threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						hasViolated = true;
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			case "==":
				if (! value_measured.equals(threshold)){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						hasViolated = true;
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			case ">=<":
				if (value_measured.equals(threshold)){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						hasViolated = true;
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			default:
				String errorMessage = String.format("Operator %s is not valid", operator);
				System.out.println(errorMessage);
				throw new ChaincodeException(errorMessage);
		}
		return new Pair<Boolean, Violation> (hasViolated, violation);
	}

	private Pair<Boolean, Violation> check_OneSideValueRule(String measureID, boolean hasViolated, String operator,  Float threshold, Float value_measured, Integer user_violation, Integer tolerance, Group group, String rule_id, String user_id, String error, LocalTime time, String deviceID){
		Violation violation = new Violation();
		switch (operator){ 
			case ">":
				if (value_measured <= threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						hasViolated = true;
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
					}
				}
				break;
			case ">=":
				if (value_measured < threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						hasViolated = true;
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			case "<":
				if (value_measured >= threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){						
						hasViolated = true;
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			case "<=":
				if (value_measured >= threshold){
					group.setUsersWarnings(user_id, rule_id, measureID);
					if (((user_violation + 1) >= tolerance) && hasViolated == false){
						hasViolated = true;
						violation = new Violation(measureID, measureID, user_id, group.getID(), deviceID, time, String.valueOf(value_measured), error);
						//group.setUsersWarnings(user_id, rule_id, 0);
					}
				}
				break;
			default:
				String errorMessage = String.format("Operator %s is not valid", operator);
				System.out.println(errorMessage);
				throw new ChaincodeException(errorMessage);
		}
		return new Pair<Boolean, Violation> (hasViolated, violation);
	}


	private Pair<Boolean, Violation> check_Range_ValueRule(String measureID, boolean hasViolated, String operator1, Float threshold1, String operator2, Float threshold2, Integer tolerance, Integer user_violation,  Float value_measured, Group group, String rule_id, String userId,  String error, LocalTime time, String deviceID){
		
		Pair<Boolean, Violation> result_one = check_OneSideValueRule(measureID, hasViolated, operator1, threshold1, value_measured, user_violation, tolerance, group, rule_id, userId, error, time, deviceID);
		if (result_one.getLeft()){
			return result_one;
		}else{
			Pair<Boolean, Violation> result_two = check_OneSideValueRule(measureID, hasViolated, operator2, threshold2, value_measured, user_violation, tolerance, group, rule_id, userId, error, time, deviceID);
			return result_two;
		}
	}

	/**
	 * Send a measure
	 * @param ctx the transaction context
	 * @param measureID the ID of the measure
	 * @param measureUUID the UUID of the measure
	 * @param values the values 
	 * @param valueNames the name of the values 
	 */
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void sendAndCheck(final Context ctx, final String measureID, final String measureUUID, final String values, final String valueNames){
		ChaincodeStub stub = ctx.getStub();

		String valueNamesString = valueNames.substring(1, valueNames.length()-1);
		valueNamesString = valueNamesString.replace("\"", "");
		List<String> valueNamesList = Arrays.asList(valueNamesString.split(","));

		String valuesString = values.substring(1, values.length()-1);
		valuesString = valuesString.replace("\"", "");
		List<String> valuesList = Arrays.asList(valuesString.split(","));
		//Cast values to float
		List<Float> valuesListFloat = new ArrayList<Float>();
		for (String value : valuesList) {
			valuesListFloat.add(Float.valueOf(value));
		}
		//Get parameter from UUID JSON string
		IDToken UUIDToken = gson.fromJson(stub.getStringState("UUID"+measureUUID), IDToken.class);
		System.out.println("UUIDToken: " + UUIDToken);

		String userId = UUIDToken.getUserid();
		String groupID = UUIDToken.getGroupid();
		String deviceID = UUIDToken.getDeviceid();

		Group group = getGroup(ctx, groupID);
		User user = getUser(ctx, userId);
		Policy policy = group.getPolicy();

		//Check if group exists
		if(group == null) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the device exists
		if(!deviceExists(ctx, deviceID)) {
			String errorMessage = String.format("Device %s does not exist", deviceID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if user is in the group
		if(!group.getMembers().containsKey(user.getID())) {
			String errorMessage = String.format("User %s is not in the group", user.getID());
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the user has the device
		if(!user.hasDevice(deviceID)) {
			String errorMessage = String.format("User %s has no device %s", user.getID(), deviceID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		GroupState state = getGroupState(ctx, group);

		if (state == GroupState.MONITORING) {
			LocalTime time = LocalTime.now().withNano(0);
			Measure measure = new Measure(user.getID(), measureID, time, valuesList, deviceID, groupID, valueNamesList);

			long duration = Duration.between(group.getWarning_reset_timestamp(), time).toMinutes();
			LocalTime new_resetTimestamp = getNewResetTimestamp(group.getWarning_reset_timestamp(), policy.getReset_Time(), time);
			if (new_resetTimestamp.isAfter(group.getWarning_reset_timestamp())){
				group.resetWarnings();
				group.setWarning_reset_timestamp(new_resetTimestamp);
				String groupState = gson.toJson(group);
				stub.putStringState("group"+groupID, groupState);
			}
	
			Map<String, List<String>> user_violations = group.getUsersWarnings(userId);

			List<String> data_monitored = measure.getValueNames();

			if (policy.getPolicyType() == PolicyType.TIME) {
				
				List<TimeRule> rules = policy.getTimeRules();
				Boolean hasViolated = false;
				for (TimeRule rule : rules){ 
					if (hasViolated == false){

						String monitoring = rule.getDataMonitored();
						if (data_monitored.contains(monitoring)){
							String rule_id = rule.getID();	
							Integer user_violation = user_violations.get(rule_id).size();
							String policySamplingInterval = rule.getSampling();
							Integer tolerance = rule.getTolerance();
							//Se è la prima misurazione, prende in considerazione il fine timer di voto
							//Alle misurazioni successive, prende in considerazione l'ultimo timer di misurazione
							Measure lastMeasure = user.getLastMeasure(monitoring);
							if (lastMeasure != null) {
								LocalTime lastTick = lastMeasure.getTimestamp();
								if(Duration.between(lastTick, time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
									group.setUsersWarnings(userId, rule_id, measureID);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(lastTick, time).toSeconds() + "",Duration.between(lastTick, time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true; 
									}
								}
							} else {
								if(Duration.between(group.getGroup_timestamp(), time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
									group.setUsersWarnings(userId, rule_id, measureID);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(group.getGroup_timestamp(), time).toSeconds() + "",Duration.between(group.getGroup_timestamp(), time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							}

						}
					}
				}
				
				if (hasViolated == false){
					String measureState = gson.toJson(measure);
					stub.putStringState("measure"+measureID, measureState);
					for (String data : data_monitored){
						user.setLastMeasure(data, measure);
					}
					String userState = gson.toJson(user);
					stub.putStringState("user"+userId, userState);
				}
			}
			else if (policy.getPolicyType() == PolicyType.VALUE) {
				
				List<ValueRule> rules = policy.getValueRules();
				Boolean hasViolated = false;
				Violation violation = new Violation();
				for (ValueRule rule : rules){ 
					
					if (hasViolated == false){
						
						String monitoring = rule.getDataMonitored();

						if (data_monitored.contains(monitoring)){
							
							String rule_id = rule.getID();	
							Integer user_violation = user_violations.get(rule_id).size();

							int index = data_monitored.indexOf(monitoring);
							List<String> values_measured = measure.getValues();
							Float value_measured = Float.valueOf(values_measured.get(index));

							Float threshold1 = rule.getThreshold1();
							String operator1 = rule.getOperator1();
							Float threshold2 = rule.getThreshold2();
							String operator2 = rule.getOperator2();
							Integer tolerance = rule.getTolerance();

							
							if (operator2.equals("") && threshold2.equals(Float.POSITIVE_INFINITY)){
								String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +" does not respect the limit " + operator1 + " " + threshold1;
								Pair<Boolean, Violation> results = check_OneOpeatorValueRule(measureID, hasViolated, operator1, threshold1, value_measured, user_violation, tolerance, group, rule_id, userId, error_message, time, deviceID);
								hasViolated = results.getLeft();
								violation = results.getRight();
							}else{
								String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +"is outside the allowed interval " + operator1 + " " + threshold1 + operator2 + threshold2;
								Pair<Boolean, Violation> results =  check_Range_ValueRule(measureID, hasViolated,operator1, threshold1, operator2, threshold2, tolerance, user_violation, value_measured, group, rule_id, userId, error_message, time, deviceID);
								hasViolated = results.getLeft();
								violation = results.getRight();
							}
						}
					}else{
						break;
					}
				}
				if (hasViolated == false){
					String measureState = gson.toJson(measure);
					stub.putStringState("measure"+measureID, measureState);
					for (String data : data_monitored){
						user.setLastMeasure(data, measure);
					}
					String userState = gson.toJson(user);
					stub.putStringState("user"+userId, userState);
				}else{
					String violationState = gson.toJson(violation);
					stub.putStringState("violation"+violation.getID(), violationState);
				}
			}
			else if (group.getPolicy().getPolicyType() == PolicyType.TIMEVALUE) {
				List<TimeRule> time_rules = policy.getTimeRules();
				List<ValueRule> value_rules = policy.getValueRules();
				Boolean hasViolated = false;
				Violation violation = new Violation();
				for (ValueRule rule : value_rules){ 
					if (hasViolated == false){
						
						String monitoring = rule.getDataMonitored();

						if (data_monitored.contains(monitoring)){
							
							String rule_id = rule.getID();	
							Integer user_violation = user_violations.get(rule_id).size();

							int index = data_monitored.indexOf(monitoring);
							List<String> values_measured = measure.getValues();
							Float value_measured = Float.valueOf(values_measured.get(index));

							Float threshold1 = rule.getThreshold1();
							String operator1 = rule.getOperator1();
							Float threshold2 = rule.getThreshold2();
							String operator2 = rule.getOperator2();
							Integer tolerance = rule.getTolerance();

							
							if (operator2.equals("") && threshold2.equals(Float.POSITIVE_INFINITY)){
								String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +" does not respect the limit " + operator1 + " " + threshold1;
								Pair<Boolean, Violation> results = check_OneOpeatorValueRule(measureID, hasViolated, operator1, threshold1, value_measured, user_violation, tolerance, group, rule_id, userId, error_message, time, deviceID);
								hasViolated = results.getLeft();
								violation = results.getRight();
							}else{
								String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +"is outside the allowed interval " + operator1 + " " + threshold1 + operator2 + threshold2;
								Pair<Boolean, Violation> results =  check_Range_ValueRule(measureID, hasViolated,operator1, threshold1, operator2, threshold2, tolerance, user_violation, value_measured, group, rule_id, userId, error_message, time, deviceID);
								hasViolated = results.getLeft();
								violation = results.getRight();
							}
						}
					}
				}
				if (hasViolated == true){
					for (TimeRule rule : time_rules){ 
						if (hasViolated == false){

							String monitoring = rule.getDataMonitored();
							if (data_monitored.contains(monitoring)){
								String rule_id = rule.getID();	
								Integer user_violation = user_violations.get(rule_id).size();	
								String policySamplingInterval = rule.getSampling();
								Integer tolerance = rule.getTolerance();
								//Se è la prima misurazione, prende in considerazione il fine timer di voto
								//Alle misurazioni successive, prende in considerazione l'ultimo timer di misurazione
								Measure lastMeasure = user.getLastMeasure(monitoring);
								if (lastMeasure != null) {
									LocalTime lastTick = lastMeasure.getTimestamp();
									if(Duration.between(lastTick, time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
										group.setUsersWarnings(userId, rule_id, measureID);
										if (((user_violation + 1) >= tolerance) && hasViolated == false){
											violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(lastTick, time).toSeconds() + "",Duration.between(lastTick, time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
											//String violationState = gson.toJson(violation);
											//stub.putStringState("violation"+violation.getID(), violationState);
											//group.setUsersWarnings(userId, rule_id, 0);
											hasViolated = true;
										}
									}
								} else {
									if(Duration.between(group.getGroup_timestamp(), time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
										group.setUsersWarnings(userId, rule_id, measureID);
										if (((user_violation + 1) >= tolerance) && hasViolated == false){
											violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(group.getGroup_timestamp(), time).toSeconds() + "",Duration.between(group.getGroup_timestamp(), time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
											//String violationState = gson.toJson(violation);
											//stub.putStringState("violation"+violation.getID(), violationState);
											//group.setUsersWarnings(userId, rule_id, 0);
											hasViolated = true;
										}
									}
								}
							}
						}
					}
				}
				if (hasViolated == false){
					String measureState = gson.toJson(measure);
					stub.putStringState("measure"+measureID, measureState);
					for (String data : data_monitored){
						user.setLastMeasure(data, measure);
					}
					String userState = gson.toJson(user);
					stub.putStringState("user"+userId, userState);
				}else{
					String violationState = gson.toJson(violation);
					stub.putStringState("violation"+violation.getID(), violationState);
				}
			} else{
				String errorMessage = String.format("The policy type is wrong!");
				System.out.println(errorMessage);
				throw new ChaincodeException(errorMessage);
			}
		} else {
			String errorMessage = String.format("The group is not in monitoring state");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void arbiter(final Context ctx, final String groupID, final String start, final String end){

		Group group = getGroup(ctx, groupID);

		//Check if group exists
		if(group == null) {
			String errorMessage = String.format("Group %s does not exist", groupID);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Check if the group is in monitoring state
		if(!group.getState().equals(GroupState.MONITORING)) {
			String errorMessage = String.format("The group is not in monitoring state");
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage);
		}

		//Take the group, check all the measures in the group between start and end and check if there are violations of any type
		ChaincodeStub stub = ctx.getStub();
		Policy policy = group.getPolicy();

		//Start and end are LocalTime
		LocalTime startLocalTime = LocalTime.parse(start);
		LocalTime endLocalTime = LocalTime.parse(end);

		QueryResultsIterator<KeyValue> results = stub.getStateByRange("measure", "measurez");
		List<Measure> measures = new ArrayList<Measure>();

		for (KeyValue result : results) {
			String measureState = result.getStringValue();
			Measure measure = gson.fromJson(measureState, Measure.class);
			if(measure.getGroupID().equals(groupID) && measure.getTimestamp().isAfter(startLocalTime) && measure.getTimestamp().isBefore(endLocalTime)){
				measures.add(measure);
			}
		}
		measures.removeAll(Collections.singleton(null)); //Remove null measures
		
		LocalTime now = LocalTime.now().withNano(0);

		//Check if there are violations
		for (Measure measure : measures) {
			

			List<String> data_monitored = measure.getValueNames();
			List<Float> valuesListFloat = new ArrayList<Float>();
			
			for (String value : measure.getValues()) {
				valuesListFloat.add(Float.valueOf(value));
			}

			LocalTime measure_time = measure.getTimestamp();
			long duration = Duration.between(group.getWarning_reset_timestamp(), measure_time).toMinutes();
			LocalTime new_resetTimestamp = getNewResetTimestamp(group.getWarning_reset_timestamp(), policy.getReset_Time(), measure_time);
			if (new_resetTimestamp.isAfter(group.getWarning_reset_timestamp())){
				group.resetWarnings();
				group.setWarning_reset_timestamp(new_resetTimestamp);
				String groupState = gson.toJson(group);
				stub.putStringState("group"+groupID, groupState);
			}

			String userId = measure.getUserID();
			User user = getUser(ctx, userId);
			String measureID = measure.getID();
			String deviceID = measure.getIDdevice();
			Map<String, List<String>> user_violations = group.getUsersWarnings(userId);

			GroupState group_state = getGroupState(ctx, group);

			if (group_state == GroupState.MONITORING) {
				

				if (null == policy.getPolicyType()) {
					String errorMessage = String.format("Policy type %s is not valid", policy.getPolicyType());
					System.out.println(errorMessage);
					throw new ChaincodeException(errorMessage);
				}
				else switch (policy.getPolicyType()) {
					case TIME:
						{
							List<TimeRule> rules = policy.getTimeRules();
							Boolean hasViolated = false;

							for (TimeRule rule : rules){

								if (hasViolated == false){

									String rule_id = rule.getID();
									Integer user_violation = user_violations.get(rule_id).size();

									String policySamplingInterval = rule.getSampling();
									Integer tolerance = rule.getTolerance();
									String monitoring = rule.getDataMonitored();

									if (data_monitored.contains(monitoring)){
										//Se è la prima misurazione, prende in considerazione il fine timer di voto
										//Alle misurazioni successive, prende in considerazione l'ultimo timer di misurazione
										Measure lastMeasure = user.getLastMeasure(monitoring);
										
										if (lastMeasure != null) {
											LocalTime lastTick = lastMeasure.getTimestamp();
											if(Duration.between(lastTick, measure_time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
												group.setUsersWarnings(userId, rule_id, measureID);
												if (((user_violation + 1) >= tolerance) && hasViolated == false){
													Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, measure_time, Duration.between(lastTick, measure_time).toSeconds() + "",Duration.between(lastTick, measure_time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
													String violationState = gson.toJson(violation);
													stub.putStringState("violation"+violation.getID(), violationState);
													//group.setUsersWarnings(userId, rule_id, 0);
													hasViolated = true;
												}
											}
										} else {
											if(Duration.between(group.getGroup_timestamp(), measure_time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
												group.setUsersWarnings(userId, rule_id, measureID);
												if (((user_violation + 1) >= tolerance) && hasViolated == false){
													Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, measure_time, Duration.between(group.getGroup_timestamp(), measure_time).toSeconds() + "",Duration.between(group.getGroup_timestamp(), measure_time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
													String violationState = gson.toJson(violation);
													stub.putStringState("violation"+violation.getID(), violationState);
													//group.setUsersWarnings(userId, rule_id, 0);
													hasViolated = true;
												}
											}
										}
										
									}
								}
								if (hasViolated == false){
									// String measureState = gson.toJson(measure);
									// stub.putStringState("measure"+measureID, measureState);
									for (String data : data_monitored){
										user.setLastMeasure(data, measure);
									}
									String userState = gson.toJson(user);
									stub.putStringState("user"+userId, userState);
								}
							}
							break;
						}
					case VALUE:
						{
							List<ValueRule> rules = policy.getValueRules();
							Boolean hasViolated = false;
							Violation violation = new Violation();
							for (ValueRule rule : rules){
								if (hasViolated == false){
									
									String monitoring = rule.getDataMonitored();

								if (data_monitored.contains(monitoring)){
									
									String rule_id = rule.getID();	
									Integer user_violation = user_violations.get(rule_id).size();

									int index = data_monitored.indexOf(monitoring);
									List<String> values_measured = measure.getValues();
									Float value_measured = Float.valueOf(values_measured.get(index));

									Float threshold1 = rule.getThreshold1();
									String operator1 = rule.getOperator1();
									Float threshold2 = rule.getThreshold2();
									String operator2 = rule.getOperator2();
									Integer tolerance = rule.getTolerance();

									
									if (operator2.equals("") && threshold2.equals(Float.POSITIVE_INFINITY)){
										String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +" does not respect the limit " + operator1 + " " + threshold1;
										Pair<Boolean, Violation> results_value = check_OneOpeatorValueRule(measureID, hasViolated, operator1, threshold1, value_measured, user_violation, tolerance, group, rule_id, userId, error_message, measure_time, deviceID);
										hasViolated = results_value.getLeft();
										violation = results_value.getRight();
									}else{
										String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +"is outside the allowed interval " + operator1 + " " + threshold1 + operator2 + threshold2;
										Pair<Boolean, Violation> results_value =  check_Range_ValueRule(measureID, hasViolated, operator1, threshold1, operator2, threshold2, tolerance, user_violation, value_measured, group, rule_id, userId, error_message, measure_time, deviceID);
										hasViolated = results_value.getLeft();
										violation = results_value.getRight();
									}
								}
							}else{
								break;
							}
						}
						if (hasViolated == false){
							//String measureState = gson.toJson(measure);
							//stub.putStringState("measure"+measureID, measureState);
							for (String data : data_monitored){
								user.setLastMeasure(data, measure);
							}
							String userState = gson.toJson(user);
							stub.putStringState("user"+userId, userState);
						}else{
							String violationState = gson.toJson(violation);
							stub.putStringState("violation"+violation.getID(), violationState);
						};
						break;
					}
					case TIMEVALUE:
						{
							List<TimeRule> time_rules = policy.getTimeRules();
							List<ValueRule> value_rules = policy.getValueRules();
							Boolean hasViolated = false;
							Violation violation = new Violation();
							for (ValueRule rule : value_rules){ 
								if (hasViolated == false){
									
									String monitoring = rule.getDataMonitored();

									if (data_monitored.contains(monitoring)){
										
										String rule_id = rule.getID();	
										Integer user_violation = user_violations.get(rule_id).size();

										int index = data_monitored.indexOf(monitoring);
										List<String> values_measured = measure.getValues();
										Float value_measured = Float.valueOf(values_measured.get(index));

										Float threshold1 = rule.getThreshold1();
										String operator1 = rule.getOperator1();
										Float threshold2 = rule.getThreshold2();
										String operator2 = rule.getOperator2();
										Integer tolerance = rule.getTolerance();

										if (operator2.equals("") && threshold2.equals(Float.POSITIVE_INFINITY)){
											String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +" does not respect the limit " + operator1 + " " + threshold1;
											Pair<Boolean, Violation> results_value = check_OneOpeatorValueRule(measureID, hasViolated, operator1, threshold1, value_measured, user_violation, tolerance, group, rule_id, userId, error_message, measure_time, deviceID);
											hasViolated = results_value.getLeft();
											violation = results_value.getRight();
										}else{
											String error_message = "Data " + monitoring + "Value " + String.valueOf(value_measured) +"is outside the allowed interval " + operator1 + " " + threshold1 + operator2 + threshold2;
											Pair<Boolean, Violation> results_value =  check_Range_ValueRule(measureID, hasViolated,operator1, threshold1, operator2, threshold2, tolerance, user_violation, value_measured, group, rule_id, userId, error_message, measure_time, deviceID);
											hasViolated = results_value.getLeft();
											violation = results_value.getRight();
										}
									}
								}
							}
							if (hasViolated == true){
								for (TimeRule rule : time_rules){ 
									if (hasViolated == false){

										String monitoring = rule.getDataMonitored();
										if (data_monitored.contains(monitoring)){
											String rule_id = rule.getID();	
											Integer user_violation = user_violations.get(rule_id).size();	
											String policySamplingInterval = rule.getSampling();
											Integer tolerance = rule.getTolerance();
											//Se è la prima misurazione, prende in considerazione il fine timer di voto
											//Alle misurazioni successive, prende in considerazione l'ultimo timer di misurazione
											Measure lastMeasure = user.getLastMeasure(monitoring);
											
											if (lastMeasure != null) {
												LocalTime lastTick = lastMeasure.getTimestamp();
												if(Duration.between(lastTick, measure_time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
													group.setUsersWarnings(userId, rule_id, measureID);
													if (((user_violation + 1) >= tolerance) && hasViolated == false){
														violation = new Violation(measureID, measureID, userId, groupID, deviceID, measure_time, Duration.between(lastTick, measure_time).toSeconds() + "",Duration.between(lastTick, measure_time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
														//String violationState = gson.toJson(violation);
														//stub.putStringState("violation"+violation.getID(), violationState);
														//group.setUsersWarnings(userId, rule_id, 0);
														hasViolated = true;
													}
												}
											} else {
												if(Duration.between(group.getGroup_timestamp(), measure_time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
													group.setUsersWarnings(userId, rule_id,measureID);
													if (((user_violation + 1) >= tolerance) && hasViolated == false){
														violation = new Violation(measureID, measureID, userId, groupID, deviceID, measure_time, Duration.between(group.getGroup_timestamp(), measure_time).toSeconds() + "",Duration.between(group.getGroup_timestamp(), measure_time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
														//String violationState = gson.toJson(violation);
														//stub.putStringState("violation"+violation.getID(), violationState);
														//group.setUsersWarnings(userId, rule_id, 0);
														hasViolated = true;
													}
												}
											}
										}
									}
								}
							}
							if (hasViolated == false){
								//String measureState = gson.toJson(measure);
								//stub.putStringState("measure"+measureID, measureState);
								for (String data : data_monitored){
									user.setLastMeasure(data, measure);
								}
								String userState = gson.toJson(user);
								stub.putStringState("user"+userId, userState);
							}else{
								String violationState = gson.toJson(violation);
								stub.putStringState("violation"+violation.getID(), violationState);
							}
						};
						break;
					default:
						String errorMessage = String.format("Policy type %s is not valid", policy.getPolicyType());
						System.out.println(errorMessage);
						throw new ChaincodeException(errorMessage);
				}
			} else {
				String errorMessage = String.format("The group is not in monitoring state");
				System.out.println(errorMessage);
				throw new ChaincodeException(errorMessage);
			}
	
		}

	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void resetWarnings(final Context ctx, final String groupID){

		ChaincodeStub stub = ctx.getStub();

		Group group = getGroup(ctx, groupID);
		Policy policy = group.getPolicy();
		long resetTimer = policy.getReset_Time();

		LocalTime start_time = group.getWarning_reset_timestamp();
		LocalTime now = LocalTime.now().withNano(0);
		long duration = Duration.between(start_time, now).toMinutes();

		if (duration >= resetTimer){
			group.resetWarnings();
			group.setWarning_reset_timestamp(now);
			String groupState = gson.toJson(group);
			stub.putStringState("group"+groupID, groupState);
		}

	}


	private LocalTime getNewResetTimestamp(final LocalTime reset_timestamp, final long resetTimer, final LocalTime measure_time){
		long m_old_duration = Duration.between(reset_timestamp, measure_time).toMinutes();
		if (m_old_duration > resetTimer){

			long time_ticks = m_old_duration % resetTimer;
			LocalTime potential_new = reset_timestamp.plusMinutes(time_ticks * resetTimer);
			return potential_new;
			
		}else{
			return reset_timestamp;
		}
	}

}

