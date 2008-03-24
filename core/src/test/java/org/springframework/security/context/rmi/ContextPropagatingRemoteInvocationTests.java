/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.context.rmi;

import junit.framework.TestCase;

import org.springframework.security.Authentication;
import org.springframework.security.TargetObject;

import org.springframework.security.context.SecurityContextHolder;

import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import org.springframework.security.util.SimpleMethodInvocation;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;


/**
 * Tests {@link ContextPropagatingRemoteInvocation} and {@link ContextPropagatingRemoteInvocationFactory}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class ContextPropagatingRemoteInvocationTests extends TestCase {
    //~ Constructors ===================================================================================================

    public ContextPropagatingRemoteInvocationTests() {
    }

    public ContextPropagatingRemoteInvocationTests(String arg0) {
        super(arg0);
    }

    //~ Methods ========================================================================================================


    protected void tearDown() throws Exception {
        super.tearDown();
        SecurityContextHolder.clearContext();
    }

    private ContextPropagatingRemoteInvocation getRemoteInvocation()
        throws Exception {
        Class clazz = TargetObject.class;
        Method method = clazz.getMethod("makeLowerCase", new Class[] {String.class});
        MethodInvocation mi = new SimpleMethodInvocation(new TargetObject(), method, new Object[] {"SOME_STRING"});

        ContextPropagatingRemoteInvocationFactory factory = new ContextPropagatingRemoteInvocationFactory();

        return (ContextPropagatingRemoteInvocation) factory.createRemoteInvocation(mi);
    }

    public void testContextIsResetEvenIfExceptionOccurs()
        throws Exception {
        // Setup client-side context
        Authentication clientSideAuthentication = new UsernamePasswordAuthenticationToken("rod", "koala");
        SecurityContextHolder.getContext().setAuthentication(clientSideAuthentication);

        ContextPropagatingRemoteInvocation remoteInvocation = getRemoteInvocation();

        try {
            // Set up the wrong arguments.
            remoteInvocation.setArguments(new Object[] {});
            remoteInvocation.invoke(TargetObject.class.newInstance());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertNull("Authentication must be null ", SecurityContextHolder.getContext().getAuthentication());
    }

    public void testNormalOperation() throws Exception {
        // Setup client-side context
        Authentication clientSideAuthentication = new UsernamePasswordAuthenticationToken("rod", "koala");
        SecurityContextHolder.getContext().setAuthentication(clientSideAuthentication);

        ContextPropagatingRemoteInvocation remoteInvocation = getRemoteInvocation();

        // Set to null, as ContextPropagatingRemoteInvocation already obtained
        // a copy and nulling is necessary to ensure the Context delivered by
        // ContextPropagatingRemoteInvocation is used on server-side
        SecurityContextHolder.clearContext();

        // The result from invoking the TargetObject should contain the
        // Authentication class delivered via the SecurityContextHolder
        assertEquals("some_string org.springframework.security.providers.UsernamePasswordAuthenticationToken false",
            remoteInvocation.invoke(new TargetObject()));
    }

    public void testNullContextHolderDoesNotCauseInvocationProblems() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null); // just to be explicit

        ContextPropagatingRemoteInvocation remoteInvocation = getRemoteInvocation();
        SecurityContextHolder.getContext().setAuthentication(null); // unnecessary, but for explicitness

        assertEquals("some_string Authentication empty", remoteInvocation.invoke(new TargetObject()));
    }
}
