/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.netflix.conductor.contribs;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.contribs.queue.ObservableQueue;
import com.netflix.conductor.contribs.queue.QueueManager;
import com.netflix.conductor.contribs.queue.sqs.SQSObservableQueue;
import com.netflix.conductor.contribs.queue.sqs.SQSObservableQueue.Builder;
import com.netflix.conductor.contribs.tasks.Wait;
import com.netflix.conductor.core.config.Configuration;


/**
 * @author Viren
 *
 */
public class ContribsModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(QueueManager.class).asEagerSingleton();
		bind(Wait.class).toInstance(new Wait());
	}

	@Provides
	public Map<Status, ObservableQueue> getQueues(Configuration config, AWSCredentialsProvider acp) {
		
		String stack = "";
		if(config.getStack() != null && config.getStack().length() > 0) {
			stack = config.getStack() + "_";
		}
		Status[] statuses = new Status[]{Status.COMPLETED, Status.FAILED};
		Map<Status, ObservableQueue> queues = new HashMap<>();
		for(Status status : statuses) {
			String queueName = config.getProperty("workflow.listener.queue.prefix", config.getAppId() + "_sqs_notify_" + stack + status.name());
			AmazonSQSClient client = new AmazonSQSClient(acp);
			Builder builder = new SQSObservableQueue.Builder().withClient(client).withQueueName(queueName);
			
			String auth = config.getProperty("workflow.listener.queue.authorizedAccounts", "");
			String[] accounts = auth.split(",");
			for(String accountToAuthorize : accounts) {
				accountToAuthorize = accountToAuthorize.trim();
				if(accountToAuthorize.length() > 0) {
					builder.addAccountToAuthorize(accountToAuthorize.trim());
				}
			}
			ObservableQueue queue = builder.build();
			queues.put(status, queue);
		}
		
		return queues;
	}
}
