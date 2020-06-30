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

package com.google.step.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.step.data.Lead;
import com.google.step.utils.EmailUtil;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to act as the webhook to receive lead data from the Google Ads server Responds to GET
 * requests with JSON with lead data.
 */
@WebServlet("/api/webhook")
public class WebhookServlet extends HttpServlet {

  private static final Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();
  private static final String ID_URL_PARAM = "id";
  private Lead myLead;

  /**
   * Accepts a POST request containing JSON in the body describing a lead from Google Ads server.
   *
   * @param request  the HTTP Request
   * @param response the HTTP Response
   * @throws IOException if an output exception occurs with the request reader
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String advertiserKeyString = request.getParameter(ID_URL_PARAM);
    if (advertiserKeyString == null || advertiserKeyString.equals("")) {
      return; //stop execution, we expect an id param in the url
    }
    Key advertiserKey = KeyFactory.stringToKey(advertiserKeyString);
    try {
      EmailUtil.sendTestEmail();
    } catch (MessagingException e) {
      System.out.println(e);
    }
    myLead = Lead.fromReader(request.getReader());
    //TODO: Add additional verification steps
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(myLead.asEntity(advertiserKey));
  }
}
