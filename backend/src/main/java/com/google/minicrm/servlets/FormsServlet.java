// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.minicrm.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.minicrm.data.Form;
import com.google.minicrm.interfaces.ClientResponse;
import com.google.minicrm.utils.AdvertiserUtil;
import com.google.minicrm.utils.UserAuthenticationUtil;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles requests to /api/forms regarding anything related to the advertiser's forms.
 */
@WebServlet("/api/forms")
public final class FormsServlet extends HttpServlet {

  private static final String ID_URL_PARAM = "id";
  private final char[] alphanumerics = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
      .toCharArray();

  /**
   * Returns JSON representing the advertiser's unique webhook and all their Form data.
   * Authentication required.
   *
   * HTTP Response Status Codes:
   * - 200 OK: Success
   * - 401 Unauthorized: if not logged in with Google
   *
   * @param request  the HTTP Request
   * @param response the HTTP Response
   * @throws IOException if an input exception occurs with the response writer or reader
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!UserAuthenticationUtil.isAuthenticated()) {
      response.sendError(401, "Log in with Google to continue."); //401 Unauthorized
      return;
    }
    String webhookUrl = generateUserWebhook(request, UserAuthenticationUtil.getCurrentUser());
    Query query = new Query(Form.KIND_NAME)
        .setAncestor(AdvertiserUtil.createAdvertiserKey(UserAuthenticationUtil.getCurrentUser()))
        .addSort("date", Query.SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery preparedQuery = datastore.prepare(query);

    List<Form> forms = StreamSupport.stream(preparedQuery.asIterable().spliterator(), false)
        .map(Form::new).collect(Collectors.toList());

    response.setContentType("application/json;");
    response.getWriter().println(new FormsResponse(webhookUrl, forms).toJson());
  }

  /**
   * Returns JSON representing the advertiser's webhook and google key Request body needs to contain
   * form_id and form_name in application/x-www-form-urlencoded or application/json as Strings.
   * Authentication required.
   *
   * HTTP Response Status Codes:
   * - 201 Created: on success
   * - 401 Unauthorized: if not logged in with Google
   * - 403 Forbidden: if the form id is already claimed and verified
   * - 415 Not Supported: if content body is not a valid type
   *
   * @param request  the HTTP Request. Expecting parameter form_id with the form_id to add
   * @param response the HTTP Response
   * @throws IOException if an input exception occurs with the response writer or reader
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!UserAuthenticationUtil.isAuthenticated()) {
      response.sendError(401, "Log in with Google to continue."); //401 Unauthorized
      return;
    }

    //read the content body
    long formId;
    String formName;
    if (request.getContentType().contains("application/x-www-form-urlencoded")) {
      formId = Long.parseLong(request.getParameter("form_id"));
      formName = request.getParameter("form_name");
    } else if (request.getContentType().contains("application/json")) {
      Gson gson = new Gson();
      Map<String, String> jsonMap = gson.fromJson(request.getReader(), Map.class);
      formId = Long.parseLong(jsonMap.get("form_id"));
      formName = jsonMap.get("form_name");
    } else {
      response.sendError(415,
          "Content type not supported. Try application/x-www-form-urlencoded or application/json.");
      return;
    }

    //query the datastore to see if the form id already is claimed
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query(Form.KIND_NAME)
        .setFilter(CompositeFilterOperator.and(FilterOperator.EQUAL.of("formId", formId),
            FilterOperator.EQUAL.of("verified", true)))
        .setKeysOnly();
    PreparedQuery queryResults = datastore.prepare(query);
    if (!queryResults.asList(FetchOptions.Builder.withDefaults())
        .isEmpty()) { //there already exists a verified form with this id
      response.sendError(403, "Form ID already claimed by another user.");
    }

    String webhookUrl = generateUserWebhook(request, UserAuthenticationUtil.getCurrentUser());
    String googleKey = generateRandomGoogleKey(20);

    //store the form entity in the datastore
    Form newForm = new Form(formId,
        formName,
        AdvertiserUtil.createAdvertiserKey(UserAuthenticationUtil.getCurrentUser()),
        googleKey,
        false);
    datastore.put(newForm.asEntity());

    response.setStatus(201); //201 Created
    response.setContentType("application/json;");
    response.getWriter().println(new WebhookResponse(webhookUrl, googleKey, formId).toJson());
  }

  /**
   * Deletes the form owned by the current user specified by the form_id specified in the request
   * headers or a url parameter.
   * Authentication required.
   *
   * HTTP Response Status Codes:
   * - 204 No Content: on successful deletion.
   * Note: returns 204 No Content even if the form to be deleted never existed in the first place.
   * Instead, guarantees that it doesn't exist anymore in the datastore.
   * - 401 Unauthorized: if not logged in with Google
   *
   * @param request  the HTTP Request
   * @param response the HTTP Response
   */
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    if (!UserAuthenticationUtil.isAuthenticated()) {
      response.sendError(401, "Log in with Google to continue."); //401 Unauthorized
      return;
    }

    long formId = Long.parseLong(request.getParameter("form_id"));
    User user = UserAuthenticationUtil.getCurrentUser();

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.delete(Form.getFormKeyFromUserAndFormId(user, formId));

    response.setStatus(204); //Success - 204 No Content
  }

  /**
   * @return the webhook for this user with a URL-Safe Key String uniquely identifying the user
   */
  private String generateUserWebhook(HttpServletRequest request, User user) {
    //generate URL-Safe Key string
    String advertiserKeyString = KeyFactory.keyToString(AdvertiserUtil.createAdvertiserKey(user));
    return request.getScheme() + "://" +
        request.getServerName() + ":" +
        request.getServerPort() + "/api/webhook?" + ID_URL_PARAM + "=" +
        advertiserKeyString;
  }

  /**
   * Generates a random Google Key of the specified length with alphanumeric characters.
   *
   * @param length the length of the Google Key
   * @return the randomly generated Google Key
   */
  private String generateRandomGoogleKey(int length) {
    Random rand = new SecureRandom();
    StringBuilder googleKeyBuilder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      googleKeyBuilder.append(alphanumerics[rand.nextInt(alphanumerics.length)]);
    }
    return googleKeyBuilder.toString();
  }

  /**
   * Represents a response to a POST request containing a user's unique webhook and
   * form_id and google_key of the form created.
   */
  private final class WebhookResponse implements ClientResponse {

    /**
     * Webhook URL for Advertiser to put in Google Ads.
     */
    private final String webhookUrl;

    /**
     * Randomly generated Google Key
     */
    private final String googleKey;

    /**
     * The id of the form
     */
    private final long formId;

    /**
     * Constructor for response to send back to user containing the webhook and google key
     *
     * @param webhookUrl url to receive lead data from Google Ads
     * @param googleKey  randomly generated google key string
     * @param formId     id of the form
     */
    WebhookResponse(String webhookUrl, String googleKey, long formId) {
      this.webhookUrl = webhookUrl;
      this.googleKey = googleKey;
      this.formId = formId;
    }

    @Override
    public String toJson() {
      Gson gson = new GsonBuilder().disableHtmlEscaping().create();
      return gson.toJson(this);
    }
  }

  /**
   * Response object providing a user's webhook and all their forms to a GET request
   */
  private final class FormsResponse implements ClientResponse {

    /**
     * Webhook URL for Advertiser to put in Google Ads.
     */
    private final String webhookUrl;

    /**
     * List of the User's forms
     */
    private final List<Form> forms;

    /**
     * Constructor for response to send back to user containing the webhook
     *
     * @param webhookUrl url to receive lead data from Google Ads
     * @param forms      list of all the user's forms
     */
    FormsResponse(String webhookUrl, List<Form> forms) {
      this.webhookUrl = webhookUrl;
      this.forms = new ArrayList<>(forms);
    }

    @Override
    public String toJson() {
      Gson gson = new GsonBuilder().disableHtmlEscaping().create();
      return gson.toJson(this);
    }
  }
}
