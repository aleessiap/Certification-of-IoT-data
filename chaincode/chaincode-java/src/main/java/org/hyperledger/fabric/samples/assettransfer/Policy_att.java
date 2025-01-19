package org.hyperledger.fabric.samples.assettransfer;

import java.util.ArrayList;
import java.util.List;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public abstract class Policy_att {

    @Property()
    private String ID;
    
    @Property()
    private PolicyType policyType;

    @Property()
    private List<String> valueNames;

    //Policy constructor with samplingInterval, valueThresholds and operatorThresholds
    public Policy_att(@JsonProperty("ID") final String ID, 
                @JsonProperty("policyType") final PolicyType policyType){

        this.ID = ID;
        this.policyType = policyType;
    }


    public PolicyType getpolicyType() {
        return policyType;
    }

    public String getID() {
        return ID;
    }

    @Override
    public String toString() {
        return "Policy{policyType='" + policyType + "', ID='" + ID + "'}";
    }

}

@DataType()
public class TimePolicy extends Policy{

    @Property()
    private String samplingInterval;

    @Property()
    private String tolerance;

    @Property()
    private String valueName;

    public String getSamplingInterval() {
        return samplingInterval;
    }

    public String getTolerance() {
        return tolerance;
    }

    public String getValueName() {
        return valueName;
    }

    public TimePolicy(@JsonProperty("ID") final String ID, 
                    @JsonProperty("policyType") final PolicyType policyType,
                    @JsonProperty("valueName") final List<String> valueName, 
                @JsonProperty("samplingInterval") final String samplingIntervals,
                @JsonProperty("toleranceTime") final String tolerance){

        super(ID, policyType);
        this.valueName = valueName;
        this.samplingInterval = samplingIntervals;
        this.tolerance = tolerance;
    }

    @Override
    public String toString() {
        return super.toString() + ", TimePolicy{ID='" + ID + "', valueName='" + valueName + "', Timevalue=" + samplingIntervals  + ", tolerance=" + tolerance + "}";
    }

}

@DataType()
public class ValuePolicy extends Policy{

    @Property()
    private List<String> valueThreshold;

    @Property()
    private String tolerance;

    @Property()
    private List<String> operatorThreshold;

    @Property()
    private String valueName;

    public String getThreshold() {
        return samplingInterval;
    }

    public String getTolerance() {
        return tolerance;
    }

    public String getOperator() {
        return operatorThreshold;
    }

    public String getValueName() {
        return valueName;
    }

    public ValuePolicy(@JsonProperty("ID") final String ID, 
                @JsonProperty("policyType") final PolicyType policyType,
                @JsonProperty("valueNames") final String valueName,
                @JsonProperty("valueThresholds") final List<String> valueThreshold,
                @JsonProperty("operatorThresholds") final List<String> operatorThreshold,
                @JsonProperty("toleranceValue") final String tolerance
                ){

        super(ID, policyType);
        this.valueName = valueName;
        this.valueThreshold = valueThreshold;
        this.operatorThreshold = operatorThreshold;
        this.tolerance = tolerance;
    }

    @Override
    public String toString() {
        return super.toString() + ", ValuePolicy{ID='" + ID + "', valueName='" + valueName + "', value=" + valueThreshold + ", operator=" + operatorThreshold + ", tolerance=" + tolerance + "}";
    }
}


@DataType
class TimeValuePolicy extends Policy {
    @Property()
    private TimePolicy timePolicy;
    @Property()
    private ValuePolicy valuePolicy;

    public TimeValuePolicy(
            @JsonProperty("ID") final String ID, 
            @JsonProperty("policyType") final PolicyType policyType, 
            @JsonProperty("samplingInterval") final String samplingIntervals, 
            @JsonProperty("toleranceTime") final String toleranceTime, 
            @JsonProperty("nameValue") final String nameValue, 
            @JsonProperty("operatorThresholds") final List<String> operatorThreshold,
            @JsonProperty("valueThresholds") final List<String> valueThreshold, 
            @JsonProperty("toleranceValue") final String toleranceValue) {
        super(policyType, ID);
        this.timePolicy = new TimePolicy(ID + '_t', policyType, nameValue, samplingIntervals, toleranceTime);
        this.valuePolicy = new ValuePolicy(ID + '_v', policyType, nameValue, valueThreshold, operatorThreshold, toleranceValue);
    }

    @Override
    public String toString() {
        return super.toString() + ", TimeValuePolicy{ID='" + ID + "', " + timePolicy.toString() + ", " + valuePolicy.toString() + "}";
    }
}



@DataType
class FinalPolicy extends Policy {
    private List<Policy> policies = new ArrayList<>();

    public FinalPolicy(PolicyType policyType, String ID) {
        super(policyType, ID);
    }

    public void addPolicy(Policy policy) {
        policies.add(policy);
    }

    @Override
    public String toString() {
        return "FinalPolicy{ID='" + this.getID() + "', policies=" + policies + "}";
    }
}


 ''' ValuePolicy ID, policyType, valueName, valuethreshold, operator, tolerance 
     TimePolicy ID, policyType, valueName, samplingInterval, tolerance
     TimeValuePolicy ID, policyType, samplingInterval, toleranceTime, nameValue, operator, valueThreshold, toleranceValue 
 '''
class PolicyFactory {

    public static Policy createPolicy(PolicyType policyType, String ID,  String[] data) {

        FinalPolicy compositePolicy = new FinalPolicy(policyType, ID);

        if (policyType == PolicyType.TIMEVALUE){
            for (int i = 0; i < data.length; i += 6) {
                String samplingInterval = data[i];
                String toleranceTime = data[i + 1];
                String nameValue = data[i + 2];
                String operator = data[i + 3];
                String valueThreshold = data[i + 4];
                String toleranceValue = data[i + 5];
                compositePolicy.addPolicy(new TimeValuePolicy(ID, policyType, samplingInterval, toleranceTime, nameValue, operator, valueThreshold, toleranceValue));
            }
           

        } else if (policyType == PolicyType.TIME){
            for (int i = 0; i < data.length; i += 3) {
                String valueName = data[i];
                String samplingInterval = data[i + 1];
                String tolerance = data[i + 2];
                compositePolicy.addPolicy(new TimePolicy(ID, policyType, valueName, samplingInterval, tolerance));
            }
            

        } else if (policyType == PolicyType.VALUE){

            for (int i = 0; i < data.length; i += 4) {
                String valueName = data[i];
                String valueThreshold = data[i + 1];
                String operator = data[i + 2];
                String tolerance = data[i + 3];
                compositePolicy.addPolicy(new ValuePolicy(ID, policyType, valueName, valuethreshold, operator, tolerance));
            }

        }
        
        return compositePolicy

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
	/*
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void sendMeasure(final Context ctx, final String measureID, final String measureUUID, final String values, final String valueNames){
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

		Map<String, Integer> user_violations = group.getUsersWarnings(userId);

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
			Policy policy = group.getPolicy();
			List<String> data_monitored = measure.getValueNames();

			if (policy.getPolicyType() == PolicyType.TIME) {
				
				List<TimeRule> rules = policy.getTimeRules();
				Boolean hasViolated = false;
				for (TimeRule rule : rules){ 
					if (hasViolated == false){

						String monitoring = rule.getDataMonitored();
						if (data_monitored.contains(monitoring)){
							String rule_id = rule.getID();	
							Integer user_violation = user_violations.get(rule_id);
							String policySamplingInterval = rule.getSampling();
							Integer tolerance = rule.getTolerance();
							//Se è la prima misurazione, prende in considerazione il fine timer di voto
							//Alle misurazioni successive, prende in considerazione l'ultimo timer di misurazione
							Measure lastMeasure = user.getLastMeasure(monitoring);
							if (lastMeasure != null) {
								LocalTime lastTick = lastMeasure.getTimestamp();
								if(Duration.between(lastTick, time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(lastTick, time).toSeconds() + "",Duration.between(lastTick, time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							} else {
								if(Duration.between(group.getGroup_timestamp(), time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(group.getTimer(), time).toSeconds() + "",Duration.between(group.getTimer(), time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
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
				for (ValueRule rule : rules){ 
					if (hasViolated == false){

						String monitoring = rule.getDataMonitored();

						if (data_monitored.contains(monitoring)){
							String rule_id = rule.getID();	
							Integer user_violation = user_violations.get(rule_id);
							Float threshold1 = rule.getThreshold1();
							String operator1 = rule.getOperator1();
							Float threshold2 = rule.getThreshold2();
							String operator2 = rule.getOperator2();
							Integer tolerance = rule.getTolerance();
							int index = data_monitored.indexOf(monitoring);
							List<String> values_measured = measure.getValues();
							Float value_measured = Float.parseFloat(values_measured.get(index));

							if (operator.equals("==")){

								if (! (value_measured.equals(threshold))){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" + String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is not equal to " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							}else if (operator.equals(">")){
								if (! (value_measured > threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is less-equal than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							} else if (operator.equals("<")){
								if (! (value_measured < threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is greater-equal than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							} else if (operator.equals(">=")) {
								if (! (value_measured >= threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is less than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							} else if (operator.equals("<=")){
								if (! (value_measured <= threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is greater than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							} else if (operator.equals(">=<")){
								if (value_measured.equals(threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is greater than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										hasViolated = true;
									}
								}
							} else {
									String errorMessage = String.format("Operator %s is not valid", operator);
									System.out.println(errorMessage);
									throw new ChaincodeException(errorMessage);
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
			else if (group.getPolicy().getPolicyType() == PolicyType.TIMEVALUE) {
				List<TimeRule> time_rules = policy.getTimeRules();
				List<ValueRule> value_rules = policy.getValueRules();
				Boolean hasViolated = false;
				for (ValueRule rule : value_rules){ 
					if (hasViolated == false){
						
						String monitoring = rule.getDataMonitored();

						if (data_monitored.contains(monitoring)){
							String rule_id = rule.getID();	
							Integer user_violation = user_violations.get(rule_id);
							Float threshold = rule.getThreshold();
							String operator = rule.getOperator();
							Integer tolerance = rule.getTolerance();

							int index = data_monitored.indexOf(monitoring);
							List<String> values_measured = measure.getValues();
							Float value_measured = Float.parseFloat(values_measured.get(index));

							if (operator.equals("==")){

								if (! (value_measured.equals(threshold))){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" + String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is not equal to " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
										hasViolated = true;
									}
								}
							}else if (operator.equals(">")){
								if (! (value_measured > threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is less-equal than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
										hasViolated = true;
									}
								}
							} else if (operator.equals("<")){
								if (! (value_measured < threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is greater-equal than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
										hasViolated = true;
									}
								}
							} else if (operator.equals(">=")) {
								if (! (value_measured >= threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is less than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
										hasViolated = true;
									}
								}
							} else if (operator.equals("<=")){
								if (! (value_measured <= threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is greater than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
										hasViolated = true;
									}
								}
							} else if (operator.equals(">=<")){
								if (value_measured.equals(threshold)){
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID + "_" +  String.valueOf(index), measureID, userId, groupID, deviceID, time, value_measured + "", "Data " + monitoring + "Value " + String.valueOf(value_measured) + " is different than " + String.valueOf(threshold));
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
										hasViolated = true;
									}
								}
							} else {
									String errorMessage = String.format("Operator %s is not valid", operator);
									System.out.println(errorMessage);
									throw new ChaincodeException(errorMessage);
							}
						}
					}
				}
				for (TimeRule rule : time_rules){ 
					if (hasViolated == false){

						String monitoring = rule.getDataMonitored();
						if (data_monitored.contains(monitoring)){
							String rule_id = rule.getID();	
							Integer user_violation = user_violations.get(rule_id);	
							String policySamplingInterval = rule.getSampling();
							Integer tolerance = rule.getTolerance();
							//Se è la prima misurazione, prende in considerazione il fine timer di voto
							//Alle misurazioni successive, prende in considerazione l'ultimo timer di misurazione
							Measure lastMeasure = user.getLastMeasure(monitoring);
							if (lastMeasure != null) {
								LocalTime lastTick = lastMeasure.getTimestamp();
								if(Duration.between(lastTick, time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(lastTick, time).toSeconds() + "",Duration.between(lastTick, time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
										hasViolated = true;
									}
								}
							} else {
								if(Duration.between(group.getTimer(), time).toSeconds() > Integer.parseInt(policySamplingInterval)) {
									group.setUsersWarnings(userId, rule_id, user_violation + 1);
									if (((user_violation + 1) >= tolerance) && hasViolated == false){
										Violation violation = new Violation(measureID, measureID, userId, groupID, deviceID, time, Duration.between(group.getTimer(), time).toSeconds() + "",Duration.between(group.getTimer(), time).toSeconds() + " seconds, instead of " + policySamplingInterval + " seconds");
										String violationState = gson.toJson(violation);
										stub.putStringState("violation"+violation.getID(), violationState);
										group.setUsersWarnings(userId, rule_id, 0);
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
	*/