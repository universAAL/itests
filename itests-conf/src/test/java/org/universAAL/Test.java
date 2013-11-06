/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 * Copyright 2013 Fraunhofer-Gesellschaft - Institute for Computer Graphics Research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.universAAL;

import java.util.Properties;

import org.universAAL.itests.conf.IntegrationTestConsts;

import junit.framework.TestCase;

/**
 * @author amedrano
 *
 */
public class Test extends TestCase {



    public void testConsts(){
	Properties p = IntegrationTestConsts.getuAALMWProperties();
	assertTrue(p.entrySet().size() > 0);
    }

}
