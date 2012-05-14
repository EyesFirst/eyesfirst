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
import org.hibernate.NonUniqueObjectException

/**
 * @author David Smiley dsmiley@mitre.org
 */
public class EfidTests extends GroovyTestCase {

    protected void setUp() {
        super.setUp()
        //mockDomain(Efid)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testUnique() {
        String id = 'EF123456780'
        def makeMe = {->
            def ef = new Efid()
            ef.id = id
            ef.save(flush:true, failOnError:true)
        }
        makeMe()
        try {
            makeMe()
            fail()
        } catch (NonUniqueObjectException e) {//expected
        }
    }

}