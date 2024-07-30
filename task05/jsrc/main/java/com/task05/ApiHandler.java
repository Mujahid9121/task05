//package com.task05;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.syndicate.deployment.annotations.lambda.LambdaHandler;
//import com.syndicate.deployment.model.RetentionSetting;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@LambdaHandler(lambdaName = "api_handler",
//	roleName = "api_handler-role",
//	isPublishVersion = true,
//	aliasName = "${lambdas_alias_name}",
//	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
//)
//public class ApiHandler implements RequestHandler<Object, Map<String, Object>> {
//
//	public Map<String, Object> handleRequest(Object request, Context context) {
//		System.out.println("Hello from lambda");
//		Map<String, Object> resultMap = new HashMap<String, Object>();
//		resultMap.put("statusCode", 200);
//		resultMap.put("body", "Hello from Lambda");
//		return resultMap;
//	}
//}
package com.task05;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = false,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private static final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		Map<String, String> content;
		try {
			content = objectMapper.readValue(request.getBody(), new TypeReference<Map<String, String>>() {});
		} catch (IOException e) {
			APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
			response.setStatusCode(400);
			response.setBody("Error parsing request body");
			return response;
		}

		String id = UUID.randomUUID().toString();
		String createdAt = java.time.Instant.now().toString();

		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue(id));
		item.put("principalId", new AttributeValue(content.get("principalId")));
		item.put("createdAt", new AttributeValue(createdAt));

		Map<String, AttributeValue> bodyMap = new HashMap<>();
		content.forEach((key, value) -> bodyMap.put(key, new AttributeValue().withS(value)));
		item.put("body", new AttributeValue().withM(bodyMap));

		PutItemRequest putItemRequest = new PutItemRequest()
				.withTableName("Events")
				.withItem(item);
		dynamoDB.putItem(putItemRequest);

		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		response.setStatusCode(201);
		response.setBody("Event created successfully with ID: " + id);
		return response;
	}
}