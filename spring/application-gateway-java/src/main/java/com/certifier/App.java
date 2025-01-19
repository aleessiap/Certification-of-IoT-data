package com.certifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import Schemas.ResponseSchema;
import Schemas.SendMeasure;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "EnergyCertifier API", version = "1.0", description = "EnergyCertifier API\n\n"
		+ "This API allows the user to interact with the EnergyCertifier blockchain network. The user can register and enroll new users, create and get users, groups, devices and policies, send requests to join groups, accept requests, start policy proposal phase, send votes and proposals."+
		"\n\nThe \"username\" parameter is the Username of the transaction-caller in the blockchain network, in order to make calls, it is necessary to have a user registered and enrolled in the blockchain network."))
@RestController
@RequestMapping("/v1/")
public class App {

	private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private static final String MSP_ID = System.getenv().getOrDefault("MSP_ID", "Org1MSP");

	// Path to crypto materials.
	private static final Path CRYPTO_PATH = Paths.get("../../blockchain/test-network/organizations/peerOrganizations/org1.example.com");
	// Path to peer tls certificate.
	private static final Path TLS_CERT_PATH = CRYPTO_PATH.resolve(Paths.get("peers/peer0.org1.example.com/tls/ca.crt"));

	// Gateway peer end point.
	private static final String PEER_ENDPOINT = "localhost:7051";
	private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

	private static HashMap<String, String> timeStamps = new HashMap<String, String>();

	
	/** 
	 * @return ManagedChannel
	 * @throws IOException
	 */
	private static ManagedChannel newGrpcConnection() throws IOException {
		var credentials = TlsChannelCredentials.newBuilder()
				.trustManager(TLS_CERT_PATH.toFile())
				.build();
		return Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
				.overrideAuthority(OVERRIDE_AUTH)
				.build();
	}

	private static Path getFirstFilePath(Path dirPath) throws IOException {
		try (var keyFiles = Files.list(dirPath)) {
			return keyFiles.findFirst().orElseThrow();
		}
	}

	ManagedChannel channel;

	public App() throws IOException {
		this.channel = newGrpcConnection();
	}

	//Send response API
	public static ResponseEntity<Object> sendResponse(String message, HttpStatus httpStatus, Object data) {
		Map<String, Object> response = new HashMap<>();
		response.put("message", message);
		response.put("status", httpStatus);
		response.put("data", data);
		return new ResponseEntity<>(response, httpStatus);
	}

	public Contract connect(String username) throws IOException, GatewayException, InvalidKeyException, Exception{
		// The gRPC client connection should be shared by all Gateway connections to
		// this endpoint.

		//Create a new Gateway connection

		Identity identity = retrieveIdentity(username);
		Signer signer = retrieveSigner(username);

		Gateway.Builder builder = Gateway.newInstance().identity(identity).signer(signer).connection(channel)
					// Default timeouts for different gRPC calls
					.evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
					.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
					.submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
					.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

			
		Gateway gateway = builder.connect();
		var network = gateway.getNetwork(CHANNEL_NAME);
		System.out.println("Gateway connection established");
		gateway.close();
		return network.getContract(CHAINCODE_NAME);
	}

	/**
	 * Register a new user in the blockchain
	 * @param ID - The ID of the user
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@PostMapping("/auth")
	@Operation(summary = "Register a new user in the blockchain")
	@ApiResponse(responseCode = "200", description = "User registered",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"User registered\", \"status\": \"OK\", \"data\": null}"
					)
			)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
					)
			)
	)
	@ApiResponse(responseCode = "500", description = "Error registering user",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error registering user\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
					)
			)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the ID of the user",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"ID\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> registerUserInBC(@RequestBody ObjectNode objectNode) throws InterruptedException, IOException{
		try{

			try {
				String ID = objectNode.get("ID").asText();
				ProcessBuilder processBuilder = new ProcessBuilder();
				processBuilder.command("bash", "-c", "cd $HOME/Desktop/EnergyCertifier-blockchain/EnergyCertifier-blockchain/blockchain/test-network && ./organizations/fabric-ca/registerUser.sh " + ID + " " + ID + "pw");
				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();

				IOUtils.copy(process.getInputStream(), System.out);
				process.waitFor();

				return sendResponse("User registered", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error registering user", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}

		} catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Enroll a user in the blockchain
	 * @param ID - The ID of the user
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@PutMapping("/auth")
	@Operation(summary = "Enroll a user in the blockchain")
	@ApiResponse(responseCode = "200", description = "User enrolled",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"User enrolled\", \"status\": \"OK\", \"data\": null}"
					)
			)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
					)
			)
	)
	@ApiResponse(responseCode = "500", description = "Error enrolling user",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error enrolling user\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
					)
			)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the ID of the user",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"ID\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> enrollUserInBC(@RequestBody ObjectNode objectNode) throws InterruptedException, IOException{
		try{
			
			try {
				String ID = objectNode.get("ID").asText();
				ProcessBuilder processBuilder = new ProcessBuilder();
				processBuilder.command("bash", "-c", "cd $HOME/Desktop/EnergyCertifier-blockchain/EnergyCertifier-blockchain/blockchain/test-network && ./organizations/fabric-ca/enrollUser.sh " + ID + " " + ID + "pw");
				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();

				IOUtils.copy(process.getInputStream(), System.out);
				process.waitFor();

				return sendResponse("User enrolled", HttpStatus.OK, null);
			}
			catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error enrolling user", HttpStatus.INTERNAL_SERVER_ERROR, null);

			}

			}
		catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}
	}

	private static Identity retrieveIdentity(String username) throws IOException, GatewayException, InvalidKeyException {
		//CERT_DIR_PATH
		Path usernamePath = CRYPTO_PATH.resolve(Paths.get("users/" + username + "@org1.example.com/msp/signcerts"));
		try (var certReader = Files.newBufferedReader(getFirstFilePath(usernamePath))) {
			var certificate = Identities.readX509Certificate(certReader);
			return new X509Identity(MSP_ID, certificate);
		} catch (Exception e) {
			throw new InvalidKeyException("Failed to retrieve identity", e);
		}
	}

	private static Signer retrieveSigner(String username) throws IOException, InvalidKeyException, InvalidKeyException {
		//KEY_DIR_PATH
		Path usernamePath = CRYPTO_PATH.resolve(Paths.get("users/" + username + "@org1.example.com/msp/keystore"));
		try (var keyReader = Files.newBufferedReader(getFirstFilePath(usernamePath))) {
			var privateKey = Identities.readPrivateKey(keyReader);
			return Signers.newPrivateKeySigner(privateKey);
		} catch (Exception e) {
			throw new InvalidKeyException("Failed to retrieve signer", e);
		}
	}

	private String prettyJson(final byte[] json) {
		return prettyJson(new String(json, StandardCharsets.UTF_8));
	}

	private String prettyJson(final String json) {
		var parsedJson = JsonParser.parseString(json);
		return gson.toJson(parsedJson);
	}

	/**
	 * Get a user from the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC and the userId
	 * @return - The user in JSON format
	 * @throws GatewayException
	 */
	@GetMapping("/users/{userId}")
	@Operation(summary = "Get a user from the blockchain")
	@ApiResponse(responseCode = "200", description = "User found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"User found\", \"status\": \"OK\", \"data\": {\"user\": \"userData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "404", description = "User not found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"User not found\", \"status\": \"NOT_FOUND\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getUser(@RequestParam String username, @PathVariable String userId) throws GatewayException {
		System.out.println("\n--> Evaluate Transaction: GetUser");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getUser", userId);
			String prettyJson = prettyJson(result);
			return sendResponse("User found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("User not found", HttpStatus.NOT_FOUND, null);
	}

	/**
	 * Get a group from the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC and the groupId
	 * @return - The group in JSON format
	 * @throws GatewayException
	 */
	@GetMapping("/groups/{groupId}")
	@Operation(summary = "Get a group from the blockchain")
	@ApiResponse(responseCode = "200", description = "Group found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Group found\", \"status\": \"OK\", \"data\": {\"group\": \"groupData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "404", description = "Group not found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Group not found\", \"status\": \"NOT_FOUND\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getGroup(@RequestParam String username, @PathVariable String groupId) throws GatewayException {
		System.out.println("\n--> Evaluate Transaction: GetGroup");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getGroup", groupId);
			String prettyJson = prettyJson(result);
			return sendResponse("Group found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Group not found", HttpStatus.NOT_FOUND, null);
	}

	
	/**
	 * Create a new user in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC, userId, name, surname and email
	 */
	@PostMapping("/users")
	@Operation(summary = "Create a new user in the blockchain")
	@ApiResponse(responseCode = "200", description = "User created",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"User created\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error creating user",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error creating user\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the userId, name, surname and email of the user",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"userId\": \"1\", \"name\": \"John\", \"surname\": \"Doe\", \"email\": \"jdoe@gmail.com\"}"
					)
			)
	)
	private ResponseEntity<Object> createUser(@RequestBody ObjectNode objectNode) {
		System.out.println("\n--> Submit Transaction: CreateUser");

		try{
			String username = objectNode.get("username").asText();
			String userId = objectNode.get("userId").asText();
			String name = objectNode.get("name").asText();
			String surname = objectNode.get("surname").asText();
			String email = objectNode.get("email").asText();


			try {
				Contract contract = connect(username);
				contract.submitTransaction("createUser", userId, name, surname, email);
				return sendResponse("User created", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error creating user", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Create a new group in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId, name, location and user
	 */
	@PostMapping("/groups")
	@Operation(summary = "Create a new group in the blockchain")
	@ApiResponse(responseCode = "200", description = "Group created",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Group created\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error creating group",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error creating group\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the groupId, name, location and user of the group",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"ID\": \"1\", \"name\": \"Group1\", \"location\": \"Location1\", \"user\": \"1\", \"proposalTimer\": \"1\", \"voteTimer\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> createGroup(@RequestBody ObjectNode objectNode) {
		System.out.println("\n--> Submit Transaction: CreateGroup");

		try{
			String username = objectNode.get("username").asText();
			String ID = objectNode.get("ID").asText();
			String name = objectNode.get("name").asText();
			String location = objectNode.get("location").asText();
			String user = objectNode.get("user").asText();
			String proposalTimer = objectNode.get("proposalTimer").asText();
			String voteTimer = objectNode.get("voteTimer").asText();

			try {
				Contract contract = connect(username);
				contract.submitTransaction("createGroup", ID, name, location, user, proposalTimer, voteTimer);
				return sendResponse("Group created", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error creating group", HttpStatus.INTERNAL_SERVER_ERROR, null);

			}

		}
		catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Create a new device in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC, ID, unit and userId
	 */
	@PostMapping("/devices")
	@Operation(summary = "Create a new device in the blockchain")
	@ApiResponse(responseCode = "200", description = "Device registered",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Device registered\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error registering device",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error registering device\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the ID, unit and userId of the device",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"ID\": \"1\", \"unit\": \"Unit1\", \"userId\": \"1\", \"groupId\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> registerDevice(@RequestBody ObjectNode objectNode) {
		System.out.println("\n--> Submit Transaction: RegisterDevice");

		try{
			String username = objectNode.get("username").asText();
			String ID = objectNode.get("ID").asText();
			String unit = objectNode.get("unit").asText();
			String userId = objectNode.get("userId").asText();
			String groupId = objectNode.get("groupId").asText();

			try {
				Contract contract = connect(username);
				byte[] UUIDstr = contract.submitTransaction("registerDevice", ID, unit, userId, groupId);
				String prettyJson = prettyJson(UUIDstr);
				return sendResponse("Device registered", HttpStatus.OK, prettyJson);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error registering device", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Send a request to join a group
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId, userId
	 */
	@PostMapping("/groups/{groupId}/requests")
	@Operation(summary = "Send a request to join a group")
	@ApiResponse(responseCode = "200", description = "Request sent",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Request sent\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error sending request",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error sending request\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the groupId and userId of the group",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"userId\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> joinGroup(@RequestBody ObjectNode objectNode, @PathVariable String groupId) {
		System.out.println("\n--> Submit Transaction: JoinGroup");

		try{
			String username = objectNode.get("username").asText();
			String userId = objectNode.get("userId").asText();

			try{
				Contract contract = connect(username);
				contract.submitTransaction("joinGroup", groupId, userId);
				return sendResponse("Request sent", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error sending request", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Accept a request to join a group
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId, userId and adminId
	 */
	@PutMapping("/groups/{groupId}/requests/{userId}/accept")
	@Operation(summary = "Accept a request to join a group")
	@ApiResponse(responseCode = "200", description = "Request accepted",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Request accepted\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error accepting request",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error accepting request\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the adminId of the group",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"adminId\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> acceptRequest(@RequestBody ObjectNode objectNode, @PathVariable String groupId, @PathVariable String userId) {
		System.out.println("\n--> Submit Transaction: AcceptRequest");

		try{
			String username = objectNode.get("username").asText();
			String adminId = objectNode.get("adminId").asText();

			try {
				Contract contract = connect(username);
				contract.submitTransaction("acceptRequest", groupId, userId, adminId);
				return sendResponse("Request accepted", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error accepting request", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Create a new policy in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC, userId, ID, 
	 * 			samplingInterval (Allowed seconds between measures),
	 * 			policyType (TIME, VALUE or TIMEVALUE),
	 * 			valueNames (List of values to be measured, in this case their names),
	 * 			valueThresholds and (List of values to be measured, in this case their thresholds),
	 * 			operatorThresholds (List of operators to be used in the comparison: <, >, <=, >=, ==, >=<)
	 */
	@PostMapping("/users/{userId}/policies")
	@Operation(summary = "Create a new policy in the blockchain")
	@ApiResponse(responseCode = "200", description = "Policy created",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Policy created\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error creating policy",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error creating policy\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the userId, ID, samplingInterval (Allowed seconds between measures), policyType (TIME, VALUE or TIMEVALUE), valueNames (List of values to be measured, in this case their names), valueThresholds and (List of values to be measured, in this case their thresholds), operatorThresholds (List of operators to be used in the comparison: <, >, <=, >=, ==, >=<)",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"ID\": \"1\", \"samplingInterval\": \"10\", \"policyType\": \"TIME\", \"valueNames\": [\"Value1\", \"Value2\"], \"valueThresholds\": [\"10\", \"20\"], \"operatorThresholds\": [\">\", \"<\"]}"
					)
			)
	)
	private ResponseEntity<Object> createPolicy(@RequestBody ObjectNode objectNode, @PathVariable String userId) {
		System.out.println("\n--> Submit Transaction: CreatePolicy");

		try{
			String username = objectNode.get("username").asText();
			String ID = objectNode.get("ID").asText();
			//String samplingInterval = objectNode.get("samplingInterval").asText();
			String policyType = objectNode.get("policyType").asText();
			//String valueNames = objectNode.get("valueNames").toString();
			//String valueThresholds = objectNode.get("valueThresholds").toString();
			//String operatorThresholds = objectNode.get("operatorThresholds").toString();
			String rules = objectNode.get("rules").toString();
			try {
				Contract contract = connect(username);
				//System.out.println("*** Transaction committed successfully:" + valueNames + " " + valueThresholds + " " + operatorThresholds);
				System.out.println("*** Transaction committed successfully:" + policyType + " " + rules);
				//contract.submitTransaction("createPolicy", userId, ID, samplingInterval, policyType, valueNames, valueThresholds, operatorThresholds);
				contract.submitTransaction("createPolicy", userId, ID, policyType, rules);
				return sendResponse("Policy created", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error creating policy", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Get a policy from the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC and the policyId
	 * @return - The policy in JSON format
	 */
	@GetMapping("/groups/{groupId}/policies/{policyId}")
	@Operation(summary = "Get a policy from the blockchain")
	@ApiResponse(responseCode = "200", description = "Policy found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Policy found\", \"status\": \"OK\", \"data\": {\"policy\": \"policyData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "404", description = "Policy not found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Policy not found\", \"status\": \"NOT_FOUND\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getPolicy(@RequestParam String username, @PathVariable String groupId, @PathVariable String policyId) {
		System.out.println("\n--> Evaluate Transaction: GetPolicy");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getPolicy", policyId);
			String prettyJson = prettyJson(result);
			return sendResponse("Policy found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Policy not found", HttpStatus.NOT_FOUND, null);
	}

	/**
	 * Start the policy proposal phase
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId and adminId
	 */
	@PostMapping("/groups/{groupId}/policies")
	@Operation(summary = "Start the policy proposal phase")
	@ApiResponse(responseCode = "200", description = "Policy proposal phase started",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Policy proposal phase started\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error starting policy proposal phase",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error starting policy proposal phase\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the groupId and adminId of the group",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"adminId\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> startPolicyProposalPhase(@RequestBody ObjectNode objectNode, @PathVariable String groupId) {
		System.out.println("\n--> Submit Transaction: StartPolicyProposalPhase");

		try{
			String username = objectNode.get("username").asText();
			String adminId = objectNode.get("adminId").asText();

			try {
				Contract contract = connect(username);
				contract.submitTransaction("startPolicyProposalPhase", groupId, adminId);
				return sendResponse("Policy proposal phase started", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error starting policy proposal phase", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Send a vote for a policy in a group
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId, userId and policyId
	 * @throws EndorseException - If the transaction is not endorsed
	 * @throws SubmitException - If the transaction is not submitted
	 * @throws CommitStatusException - If the transaction is not committed
	 * @throws CommitException
	 */
	@PostMapping("/groups/{groupId}/votes")
	@Operation(summary = "Send a vote for a policy in a group")
	@ApiResponse(responseCode = "200", description = "Vote sent",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Vote sent\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error sending vote",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error sending vote\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the groupId, userId and policyId of the group",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"userId\": \"1\", \"policyId\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> vote(@RequestBody ObjectNode objectNode, @PathVariable String groupId) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit Transaction: Vote");

		try{
			String username = objectNode.get("username").asText();
			String userId = objectNode.get("userId").asText();
			String policyId = objectNode.get("policyId").asText();

			try {
				Contract contract = connect(username);
				contract.submitTransaction("vote", groupId, userId, policyId);
				return sendResponse("Vote sent", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error sending vote", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		}catch (Exception e){
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}

	/**
	 * Send a proposal for all policy of a user in a group
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId and userId
	 */
	@PutMapping("/groups/{groupId}/policies")
	@Operation(summary = "Send a proposal for all policy of a user in a group")
	@ApiResponse(responseCode = "200", description = "Proposal sent",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Proposal sent\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error sending proposal",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error sending proposal\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the groupId and userId of the group",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\", \"userId\": \"1\", \"policyId\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> sendProposal(@RequestBody ObjectNode objectNode, @PathVariable String groupId) {
		System.out.println("\n--> Submit Transaction: SendProposal");

		try{
			String username = objectNode.get("username").asText();
			String userId = objectNode.get("userId").asText();
			String policyId = objectNode.get("policyId").asText();

			try {
				Contract contract = connect(username);
				contract.submitTransaction("sendProposal", groupId, userId, policyId);
				return sendResponse("Proposal sent", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
				return sendResponse("Error sending proposal", HttpStatus.INTERNAL_SERVER_ERROR, null);
			}
		} catch (Exception e){
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

	}
	
	/**
	 * Send a Measure in a group
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId, userId, 
	 * and parameters of the measure like:
	 * measureId, deviceId, values (The values of the measure), valueNames (The names of the values)
	 */
	@PostMapping("/measures")
	@Operation(summary = "Send a measure, in order to prevent time-conflict it will try to send the measure 5 times before returning an error")
	@ApiResponse(responseCode = "200", description = "Measure sent",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Measure sent\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "429", description = "Too many requests",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Too many requests\", \"status\": \"TOO_MANY_REQUESTS\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error sending measure",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error sending measure\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = SendMeasure.class)
			)
	)
	private ResponseEntity<Object> sendMeasure(@RequestBody ObjectNode objectNode) {
	  										 //@RequestBody SendMeasure schema) {
		System.out.println("\n--> Submit Transaction: SendMeasure");

		Integer trials = 0;
		Boolean error = false;
		try{
			String username = objectNode.get("username").asText();
			String measureUUID = objectNode.get("measureUUID").asText();
			String values = objectNode.get("values").toString();
			String valueNames = objectNode.get("valueNames").toString();
			String measureId = UUID.randomUUID().toString();
			//If MVC conflict, rise 429 for too many requests
		
			while (trials < 5) {
				error = false;
				try {
					Contract contract = connect(username);
					LocalTime timestamp = LocalTime.now();
					timeStamps.put(measureId, timestamp.toString());
					//contract.submitTransaction("sendMeasure", measureId, measureUUID, values, valueNames);
					contract.submitTransaction("sendAndCheck", measureId, measureUUID, values, valueNames);
				} catch (Exception e) {
					e.printStackTrace();
					timeStamps.remove(measureId);
					trials++;
					error = true;
				}
		
				System.out.println("*** Measure sent, waiting for confirmation");
		
				if (!error) {
					////Check if the measure was actually sent
					try {
						Contract contract = connect(username);
						LocalTime timestamp = LocalTime.now();
						timeStamps.put(measureId, timestamp.toString());
						var result = contract.evaluateTransaction("getMeasure", measureId);
						System.out.println("*** Measure sent successfully");
						return sendResponse("Measure sent", HttpStatus.OK, prettyJson(result));
					} catch (Exception e) {
						e.printStackTrace();
						timeStamps.remove(measureId);
						trials++;
					}
				}
			}
		
			return sendResponse("Error sending measure", HttpStatus.INTERNAL_SERVER_ERROR, null);
		} catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}
		
		//return null;
	}

	/**
	 * Get a measure from the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC and the measureId
	 * @return - The measure in JSON format
	 */
	@GetMapping("/measures/{measureId}")
	@Operation(summary = "Get a measure from the blockchain")
	@ApiResponse(responseCode = "200", description = "Measure found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Measure found\", \"status\": \"OK\", \"data\": {\"measure\": \"measureData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "404", description = "Measure not found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Measure not found\", \"status\": \"NOT_FOUND\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getMeasure(@RequestParam String username, @PathVariable String measureId) {
		System.out.println("\n--> Evaluate Transaction: GetMeasure");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getMeasure", measureId);
			String prettyJson = prettyJson(result);
			return sendResponse("Measure found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Measure not found", HttpStatus.NOT_FOUND, null);
	}

	/**
	 * Get violations in a group from the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC and the groupId
	 * @return - The violations in JSON format
	 */
	@GetMapping("/groups/{groupId}/violations/{violationId}")
	@Operation(summary = "Get violations in a group from the blockchain")
	@ApiResponse(responseCode = "200", description = "Violation found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Violation found\", \"status\": \"OK\", \"data\": {\"violation\": \"violationData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "404", description = "Violation not found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Violation not found\", \"status\": \"NOT_FOUND\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getViolation(@RequestParam String username, @PathVariable String groupId, @PathVariable String violationId) {
		System.out.println("\n--> Evaluate Transaction: GetViolation");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getViolation", groupId, violationId);
			String prettyJson = prettyJson(result);
			return sendResponse("Violation found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Violation not found", HttpStatus.NOT_FOUND, null);

	}

	/*
	@PostMapping("/groups/{groupId}/violations")
	@Operation(summary = "Arbiter checks if a violation is present in a group")
	@ApiResponse(responseCode = "200", description = "Violation checked",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Violation checked\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error checking violation",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error checking violation\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the username of the caller in BC",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							
							value = "{\"username\": \"1\"}"
					)
			)
	)
	private ResponseEntity<Object> checkViolation(@PathVariable String groupId, @RequestBody ObjectNode objectNode) {
		System.out.println("\n--> Submit Transaction: getViolationsInGroup");
	
		String username = objectNode.get("username").asText();
	
		try{
			Contract contract = connect(username);
			contract.submitTransaction("getViolationsInGroup", groupId);
			return sendResponse("Violation checked", HttpStatus.OK, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return sendResponse("Error checking violation", HttpStatus.INTERNAL_SERVER_ERROR, null);
	}
	*/
	/**
	 * Reset a group in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC and the groupId
	 */
	@DeleteMapping("/groups/{groupId}")
	@Operation(summary = "Reset a group in the blockchain")
	@ApiResponse(responseCode = "200", description = "Group reset",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Group reset\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error resetting group",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error resetting group\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> resetGroup(@RequestParam String username, @PathVariable String groupId) {
		System.out.println("\n--> Submit Transaction: ResetGroup");

		try {
			Contract contract = connect(username);
			contract.submitTransaction("resetGroup", groupId);
			timeStamps.clear();
			return sendResponse("Group reset", HttpStatus.OK, null);
		} catch (Exception e) {
			e.printStackTrace();
			return sendResponse("Error resetting group", HttpStatus.INTERNAL_SERVER_ERROR, null);
		}
	}

	/**
	 * Get all the elements in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC
	 * @return - All the elements in JSON format
	 */
	@GetMapping("/")
	@Operation(summary = "Get all the elements in the blockchain")
	@ApiResponse(responseCode = "200", description = "All elements given",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"All elements found\", \"status\": \"OK\", \"data\": {\"elements\": \"elementsData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error getting all elements",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error getting all elements\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getAll(@RequestParam String username) {
		System.out.println("\n--> Evaluate Transaction: GetAll");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getAll");
			String prettyJson = prettyJson(result);
			return sendResponse("All elements found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Error getting all elements", HttpStatus.INTERNAL_SERVER_ERROR, null);
	}

	/**
	 * Get all the users in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC
	 * @return - All the users in JSON format
	 */
	@GetMapping("/users")
	@Operation(summary = "Get all the users in the blockchain")
	@ApiResponse(responseCode = "200", description = "All users given",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"All users found\", \"status\": \"OK\", \"data\": {\"users\": \"usersData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error getting all users",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error getting all users\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getAllUsers(@RequestParam String username) {
		System.out.println("\n--> Evaluate Transaction: GetAllUsers");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getAllUsers");
			String prettyJson = prettyJson(result);
			return sendResponse("All users found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Error getting all users", HttpStatus.INTERNAL_SERVER_ERROR, null);
	}

	/**
	 * Get all the groups in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC
	 * @return - All the groups in JSON format
	 */
	@GetMapping("/groups")
	@Operation(summary = "Get all the groups in the blockchain")
	@ApiResponse(responseCode = "200", description = "All groups given",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"All groups found\", \"status\": \"OK\", \"data\": {\"groups\": \"groupsData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error getting all groups",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error getting all groups\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getAllGroups(@RequestParam String username) {
		System.out.println("\n--> Evaluate Transaction: GetAllGroups");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getAllGroups");
			String prettyJson = prettyJson(result);
			return sendResponse("All groups found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Error getting all groups", HttpStatus.INTERNAL_SERVER_ERROR, null);
	}

	/**
	 * Get all measures in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC
	 * @return - All the measures in JSON format
	 */
	@GetMapping("/measures")
	@Operation(summary = "Get all the measures in the blockchain")
	@ApiResponse(responseCode = "200", description = "All measures given",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"All measures found\", \"status\": \"OK\", \"data\": {\"measures\": \"measuresData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error getting all measures",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error getting all measures\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getAllMeasures(@RequestParam String username) {
		System.out.println("\n--> Evaluate Transaction: GetAllMeasures");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getAllMeasures");
			String prettyJson = prettyJson(result);
			return sendResponse("All measures found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Error getting all measures", HttpStatus.INTERNAL_SERVER_ERROR, null);
	}

	/**
	 * Get all violations in the blockchain
	 * @param objectNode - A JSON object containing the username of the caller in BC
	 * @return - All the violations in JSON format
	 */
	@GetMapping("/violations")
	@Operation(summary = "Get all the violations in the blockchain")
	@ApiResponse(responseCode = "200", description = "All violations given",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"All violations found\", \"status\": \"OK\", \"data\": {\"violations\": \"violationsData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error getting all violations",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(							
							value = "{\"message\": \"Error getting all violations\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getAllViolations(@RequestParam String username) {
		System.out.println("\n--> Evaluate Transaction: GetAllViolations");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getAllViolations");
			String prettyJson = prettyJson(result);
			return sendResponse("All violations found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Error getting all violations", HttpStatus.INTERNAL_SERVER_ERROR, null);
	}

	/**
	 * Get all the violations in a group
	 * @param objectNode - A JSON object containing the username of the caller in BC and the groupId
	 * @return - All the violations in JSON format
	 */
	@GetMapping("/groups/{groupId}/violations")
	@Operation(summary = "Get all the violations in a group")
	@ApiResponse(responseCode = "200", description = "Violations found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Violations found\", \"status\": \"OK\", \"data\": {\"violations\": \"violationsData\"}}"
							)
					)
	)
	@ApiResponse(responseCode = "404", description = "Violations not found",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Violations not found\", \"status\": \"NOT_FOUND\", \"data\": null}"
							)
					)
	)
	private ResponseEntity<Object> getViolationsInGroup(@RequestParam String username, @PathVariable String groupId) {
		System.out.println("\n--> Evaluate Transaction: GetViolationsInGroup");

		try {
			Contract contract = connect(username);
			var result = contract.evaluateTransaction("getViolationsInGroup", groupId);
			String prettyJson = prettyJson(result);
			return sendResponse("Violations found", HttpStatus.OK, prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Violations not found", HttpStatus.NOT_FOUND, null);
	}

	/**
	 * Get the timestamps of the measures saved in Spring
	 * @return - The timestamps in JSON format
	 */
	@Operation(summary = "Get the timestamps of the measures saved in Spring")
	@ApiResponse(responseCode = "200", description = "String of timestamps found")
	@GetMapping("/tests/timestamps")
	private String getTimestampsSpring() {
		return gson.toJson(timeStamps);
	}


	
	 /**
	 * Send a Measure without checks
	 * @param objectNode - A JSON object containing the username of the caller in BC, groupId, userId,
	 * and parameters of the measure like:
	 * measureId, deviceId, values (The values of the measure), valueNames (The names of the values)
	 */
	@PostMapping("/measure_notcheck")
	@Operation(summary = "Send a measure without analyzing it, in order to prevent time-conflict it will try to send the measure 5 times before returning an error")
	@ApiResponse(responseCode = "200", description = "Measure sent",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Measure sent\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "400", description = "Malformed JSON",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Malformed JSON\", \"status\": \"BAD_REQUEST\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "429", description = "Too many requests",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Too many requests\", \"status\": \"TOO_MANY_REQUESTS\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error sending measure",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Error sending measure\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = SendMeasure.class)
			)
	)
	private ResponseEntity<Object> sendMeasureNoCheck(@RequestBody ObjectNode objectNode) {
	  										 //@RequestBody SendMeasure schema) {
		System.out.println("\n--> Submit Transaction: SendMeasureNoCheck");

		Integer trials = 0;
		Boolean error = false;
		try{
			String username = objectNode.get("username").asText();
			String measureUUID = objectNode.get("measureUUID").asText();
			String values = objectNode.get("values").toString();
			String valueNames = objectNode.get("valueNames").toString();
			String measureId = UUID.randomUUID().toString();
			//If MVC conflict, rise 429 for too many requests
  
			while (trials < 5) {
				error = false;
				try {
					Contract contract = connect(username);
					LocalTime timestamp = LocalTime.now();
					timeStamps.put(measureId, timestamp.toString());
					contract.submitTransaction("sendMeasureNoCheck", measureId, measureUUID, values, valueNames);
				} catch (Exception e) {
					e.printStackTrace();
					timeStamps.remove(measureId);
					trials++;
					error = true;
				}

				System.out.println("*** Measure sent, waiting for confirmation");

				if (!error) {
					////Check if the measure was actually sent
					try {
						Contract contract = connect(username);
						LocalTime timestamp = LocalTime.now();
						timeStamps.put(measureId, timestamp.toString());
						var result = contract.evaluateTransaction("getMeasure", measureId);
						System.out.println("*** Measure sent successfully");
						return sendResponse("Measure sent", HttpStatus.OK, prettyJson(result));
					} catch (Exception e) {
						e.printStackTrace();
						timeStamps.remove(measureId);
						trials++;
					}
				}
			}

			return sendResponse("Error sending measure", HttpStatus.INTERNAL_SERVER_ERROR, null);
		} catch (Exception e) {
			e.printStackTrace();
			//Malformed JSON
			return sendResponse("Malformed JSON", HttpStatus.BAD_REQUEST, null);
		}

		//return null;
	}


	@PostMapping("/groups/{groupId}/check_violations_complete")
	@Operation(summary = "Arbiter checks if there is a violation in a group of transactions")
	@ApiResponse(responseCode = "200", description = "Violation checked",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Violation checked\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error checking violation",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Error looking for violations\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the groupID of the caller in BC",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(

							value = "{\"groupId\": \"1\"}"
					)
			)
	)

		private ResponseEntity<Object> checkViolationComplete(@PathVariable String groupId, @RequestBody ObjectNode objectNode) {
		System.out.println("\n--> Submit Transaction: arbiter");


		try{
			String username = objectNode.get("username").asText();
			String start = objectNode.get("start").asText();
			String end = objectNode.get("end").asText();
			Contract contract = connect(username);

			contract.submitTransaction("checkViolationComplete", groupId, start, end);
			return sendResponse("Group of transaction checked for violations", HttpStatus.OK, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sendResponse("Error checking violation", HttpStatus.INTERNAL_SERVER_ERROR, null);
	}


	@PostMapping("/groups/{groupId}/reset_warnings")
	@Operation(summary = "Resetting the warnings in a group")
	@ApiResponse(responseCode = "200", description = "Warning resetted",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Warning resetted\", \"status\": \"OK\", \"data\": null}"
							)
					)
	)
	@ApiResponse(responseCode = "500", description = "Error resetting the warnings",
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseSchema.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
							value = "{\"message\": \"Error resetting the warnigns\", \"status\": \"INTERNAL_SERVER_ERROR\", \"data\": null}"
							)
					)
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "A JSON object containing the groupID of the caller in BC",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ObjectNode.class),
					examples = @io.swagger.v3.oas.annotations.media.ExampleObject(

							value = "{\"groupId\": \"1\"}"
					)
			)
	)

		private ResponseEntity<Object> resetWarnings(@PathVariable String groupId, @RequestBody ObjectNode objectNode){
			System.out.println("\n--> Submit Transaction: arbiter");

			String username = objectNode.get("groupId").asText();

			try{
				Contract contract = connect(username);

				String start = objectNode.get("start").asText();
				String end = objectNode.get("end").asText();
				contract.submitTransaction("resetWarnings", groupId);
				return sendResponse("Group of transaction checked for violations", HttpStatus.OK, null);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return sendResponse("Error checking violation", HttpStatus.INTERNAL_SERVER_ERROR, null);
		}
}
