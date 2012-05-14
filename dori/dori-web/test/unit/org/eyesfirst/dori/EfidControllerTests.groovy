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
package org.eyesfirst.dori

import grails.test.*
import grails.plugins.springsecurity.SpringSecurityService
import java.security.SecureRandom

class EfidControllerTests extends ControllerUnitTestCase {
    protected void setUp() {
        super.setUp()

        controller.secureRandom = new SecureRandom()

        mockDomain(Efid)
        mockDomain(EfidIssuer, [new EfidIssuer(name: 'MITRE', maxEfids: 1)])
        mockDomain(User, [new User(username: 'dsmiley', efidIssuer: EfidIssuer.findByName('MITRE'))])

        def sssCtrl = mockFor(SpringSecurityService)
        sssCtrl.demand.getCurrentUser(1..4) {-> User.findByUsername('dsmiley') }
        controller.springSecurityService = sssCtrl.createMock()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testIssue() {
        assertEquals 0, EfidIssuer.findByName('MITRE').efids
        assertEquals 0, Efid.count()

        controller.issue()
        def result = controller.response.contentAsString
        println result
        assertEquals 1, EfidIssuer.findByName('MITRE').efids
        assertNotNull Efid.findById(result)
        assertEquals 1, Efid.count()
        assertEquals Efid.list()[0].id, result
    }

    void testIssueTooMany() {
        controller.issue()
        assertEquals 1, EfidIssuer.findByName('MITRE').efids
        try {
            controller.issue()//might fail with validation exception, or not but efids is still 1
            assertEquals 1, EfidIssuer.findByName('MITRE').efids
        } catch (grails.validation.ValidationException e) {//expected
        }
//Grails test framework doesn't rollback changes, apparently
//        assert Efid.list().size() == 1
//        assert EfidIssuer.findByName('MITRE').efids == 1
    }

    void testVerify() {
        controller.params.id = controller.generateNewEfid()
        def found = controller.verify()
        assertEquals 404, controller.response.status

        controller.issue()
        def efid = controller.response.contentAsString

        controller.params.id = efid
        controller.verify()
        assertEquals 200, controller.response.status
    }

    void testGenerateNewEfid() {
        def set = new HashSet()
        boolean foundALeadingZero = false
        for(i in 1..64) {
            String efId = controller.generateNewEfid()
            println efId;
            assertTrue efId ==~ controller.EFID_PTN
            foundALeadingZero = foundALeadingZero || efId ==~ /EF0.*/
            assertTrue set.add(efId)
        }
        assert foundALeadingZero : "No leading 0s... not a bug unless this happens routinely."
    }
}
