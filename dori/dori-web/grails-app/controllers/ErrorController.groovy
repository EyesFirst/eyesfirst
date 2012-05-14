/*
 * Copyright 2012 The MITRE Corporation
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
import grails.plugins.springsecurity.Secured
import javax.servlet.http.HttpServletResponse

@Secured (['permitAll'])
class ErrorController {
    def error403 = {
        // Ensure that the correct response code gets sent. If we don't do
        // this, it may send a 500 (internal server error) response because
        // of the way we had to configure the UrlMappings for handling specific
        // exception types with 500.
        return response.sendError(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN)
    }
}