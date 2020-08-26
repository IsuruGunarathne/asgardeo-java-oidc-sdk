/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.asgardio.java.oidc.sdk.request;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.ServletUtils;
import io.asgardio.java.oidc.sdk.SSOAgentConstants;
import io.asgardio.java.oidc.sdk.config.model.OIDCAgentConfig;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@PrepareForTest({AuthorizationResponse.class})
public class OIDCRequestResolverTest {

    @Mock
    OIDCAgentConfig oidcAgentConfig;

    @Mock
    HttpServletRequest request;

    @BeforeMethod
    public void setUp() {

        oidcAgentConfig = mock(OIDCAgentConfig.class);
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void testIsError() {

        when(request.getParameter(SSOAgentConstants.ERROR)).thenReturn("error");
        OIDCRequestResolver resolver = new OIDCRequestResolver(request, oidcAgentConfig);
        assertTrue(resolver.isError());
    }

    @Test
    public void testIsAuthorizationCodeResponse() throws IOException, ParseException {

        MockedStatic<AuthorizationResponse> mockedAuthorizationResponse = mockStatic(AuthorizationResponse.class);
        MockedStatic<ServletUtils> mockedServletUtils = mockStatic(ServletUtils.class);
        HTTPRequest httpRequest = mock(HTTPRequest.class);
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);

        when(ServletUtils.createHTTPRequest(request)).thenReturn(httpRequest);
        when(AuthorizationResponse.parse(httpRequest)).thenReturn(authorizationResponse);
        when(authorizationResponse.indicatesSuccess()).thenReturn(true);

        OIDCRequestResolver resolver = new OIDCRequestResolver(request, oidcAgentConfig);
        assertTrue(resolver.isAuthorizationCodeResponse());
        mockedAuthorizationResponse.close();
        mockedServletUtils.close();
    }

    @Test
    public void testIsLogoutURL() {

        OIDCAgentConfig config = new OIDCAgentConfig();
        config.setLogoutURL("logout");
        when(request.getRequestURI()).thenReturn("sampleContext/logout");

        OIDCRequestResolver resolver = new OIDCRequestResolver(request, config);
        assertTrue(resolver.isLogoutURL());
    }

    @Test
    public void testIsSkipURI() {

        OIDCAgentConfig config = new OIDCAgentConfig();
        Set<String> skipURIs = new HashSet<String>();
        skipURIs.add("sampleSkipURI1");
        skipURIs.add("sampleSkipURI2");
        config.setSkipURIs(skipURIs);
        when(request.getRequestURI()).thenReturn("sampleSkipURI1");

        OIDCRequestResolver resolver = new OIDCRequestResolver(request, config);
        assertTrue(resolver.isSkipURI());
    }

    @Test
    public void testIsCallbackResponse() throws URISyntaxException {

        OIDCAgentConfig config = new OIDCAgentConfig();
        URI callbackURL = new URI("http://test/sampleCallback");
        config.setCallbackUrl(callbackURL);
        when(request.getRequestURI()).thenReturn("sampleContext/sampleCallback");

        OIDCRequestResolver resolver = new OIDCRequestResolver(request, config);
        assertTrue(resolver.isCallbackResponse());
    }

    @DataProvider
    public Object[][] getIndexPageData() {

        return new Object[][]{
                {"index.html", "sampleContext", "index.html"},
                {"", "sampleContext", "sampleContext"}
        };
    }

    @Test(dataProvider = "getIndexPageData")
    public void testGetIndexPage(String indexPageConfig, String contextPath, String expected) {

        OIDCAgentConfig config = new OIDCAgentConfig();
        config.setIndexPage(indexPageConfig);
        when(request.getContextPath()).thenReturn(contextPath);

        OIDCRequestResolver resolver = new OIDCRequestResolver(request, config);
        assertEquals(resolver.getIndexPage(), expected);
    }

    @AfterMethod
    public void tearDown() {

    }
}
