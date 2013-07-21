/**
 * Copyright (C) 2013 Marcel Hanser & Martin Koelbl <nanohome.de@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kn.uni.gis.foxhunt.context;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

public final class HttpContext {

	private static final String UTF_8 = "UTF-8";

	private static final HttpParams PARAMS = new BasicHttpParams();
	static {
		HttpConnectionParams.setConnectionTimeout(PARAMS, 5000);
		HttpConnectionParams.setSoTimeout(PARAMS, 5000);
	}
	private static final HttpContext INSTANCE = new HttpContext();

	private final DefaultHttpClient httpClient = new DefaultHttpClient(PARAMS);

	private static final ExecutorService service = Executors
			.newFixedThreadPool(3);

	private HttpContext() {
	}

	public static HttpContext getInstance() {
		return INSTANCE;
	};

	public void shutdown() {
		httpClient.getConnectionManager().shutdown();
	}

	public void get(String url, EntityHandler entityHandler) {
		send(url, null, entityHandler, HttpRequest.GET);
	}

	public void put(String url, Object body, EntityHandler entityHandler) {
		send(url, body, entityHandler, HttpRequest.PUT);
	}

	public void post(String url, Object body, EntityHandler entityHandler) {
		send(url, body, entityHandler, HttpRequest.POST);
	}

	private void send(final String url, final Object body,
			final EntityHandler entityHandler, final HttpRequest request) {
		try {
			service.submit(new Runnable() {

				@Override
				public void run() {
					try {
						HttpResponse execute = httpClient.execute(request
								.createRequest(url, body));
						HttpEntity entity = execute.getEntity();
						entityHandler.handleEntity(entity, execute
								.getStatusLine().getStatusCode());

						try {
							entity.consumeContent();
						} catch (Exception e) {
						}

					} catch (IOException e) {
						entityHandler.handleException(e);
					}
				}
			}).get();
		} catch (InterruptedException e) {
			entityHandler.handleException(e);
		} catch (ExecutionException e) {
			entityHandler.handleException(e);
		}
	}

	private enum HttpRequest implements HttpRequestFactory {
		GET {
			@Override
			public HttpUriRequest createRequest(String url, Object body) {
				return new HttpGet(url);
			}
		},
		PUT {
			@Override
			public HttpUriRequest createRequest(String url, Object body) {
				HttpPut httpPut = new HttpPut(url);
				try {
					if (body != null) {
						httpPut.setEntity(new StringEntity(XmlUtil
								.marshall(body), UTF_8));
					}
					return httpPut;
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		},
		POST {
			@Override
			public HttpUriRequest createRequest(String url, Object body) {
				HttpPost httpPost = new HttpPost(url);
				try {
					StringEntity stringEntity = new StringEntity(
							XmlUtil.marshall(body), HTTP.UTF_8);
					// Log.i(HttpContext.class.getName(),
					// "sending: " + XmlUtil.marshall(body));
					stringEntity.setContentType("text/xml");
					httpPost.setEntity(stringEntity);
					return httpPost;
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		};

	}

	private interface HttpRequestFactory {
		HttpUriRequest createRequest(String url, Object body);
	}

	public interface EntityHandler {
		void handleEntity(HttpEntity entity, int statusCode);

		void handleException(Exception exception);
	}

	public static class EntityHandlerAdapter implements EntityHandler {
		@Override
		public void handleException(Exception exception) {

		}

		@Override
		public void handleEntity(HttpEntity entity, int statusCode) {

		};
	}
}
