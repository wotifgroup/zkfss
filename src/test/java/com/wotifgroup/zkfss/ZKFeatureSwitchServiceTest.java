/*
 * #%L
 * Zookeeper Feature Switch Service
 * %%
 * Copyright (C) 2014 - 2015 The Wotif Group
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.wotifgroup.zkfss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;

public class ZKFeatureSwitchServiceTest {

    private static final byte[] FALSE = "false".getBytes();
    private static final byte[] TRUE = "true".getBytes();
    private static final String FEATURE_SWITCH1 = "Test";
    private static final String FEATURE_SWITCH2 = "blah";
    private static final String TEST_APPLICATION_NAME = "XYZ";
    private static final String TEST_FEATURE_SWITCH_NAMESPACE_NODE = "/foo";
    private static final String TEST_FEATURE_SWITCH_NAMESPACE = TEST_FEATURE_SWITCH_NAMESPACE_NODE + "/";
    private static final String ZK_PATH_FS2 = TEST_FEATURE_SWITCH_NAMESPACE + FEATURE_SWITCH2;
    private TestingServer ts;
    private String hostname;

    @Before
    public void setup() throws Exception {
        ts = new TestingServer(2181);
        hostname = java.net.InetAddress.getLocalHost().getHostName();
    }

    @After
    public void teardown() throws IOException {
        ts.close();
    }

    @Test
    public void testIsEnabledForNoNamespaceOverride() throws Exception {
        ZKFeatureSwitchService cfs = new ZKFeatureSwitchService().start();
        assertFalse(cfs.isEnabled(FEATURE_SWITCH2));
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.setData().forPath("/zkfss/" + FEATURE_SWITCH2, TRUE);

        // no easy way to sync on change - just wait for a bit
        Thread.sleep(200);

        assertTrue(cfs.isEnabled(FEATURE_SWITCH2));

        cfs.stop();
    }

    @Test
    public void testIsEnabledForFeatureNotSet() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE).start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);

        assertFalse(cfs.isEnabled(FEATURE_SWITCH1));
        cfs.stop();
    }

    @Test
    public void testIsEnabledForFeatureSet() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE).start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);

        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, TRUE);
        assertTrue(cfs.isEnabled(FEATURE_SWITCH2));
        curatorFrameworkClient.setData().forPath(ZK_PATH_FS2, FALSE);

        // no easy way to sync on change - just wait for a bit
        Thread.sleep(200);

        assertFalse(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test
    public void testHostnameOverrideForDisableSubKey() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE).disableHostnameSubKey()
                        .start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, TRUE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + hostname, FALSE);
        assertTrue(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test
    public void testHostnameOverrideForEnableSubKey() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE).enableHostnameSubKey()
                        .start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, TRUE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + hostname, FALSE);
        assertFalse(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test
    public void testApplicationNameOverrideWithDisabledHostnameSubKey() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE)
                        .setApplicationName(TEST_APPLICATION_NAME).disableHostnameSubKey().start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, TRUE);
        assertTrue(cfs.isEnabled(FEATURE_SWITCH2));
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME, FALSE);

        // no easy way to sync on change - just wait for a bit
        Thread.sleep(200);
        assertFalse(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test
    public void testApplicationNameOverrideWithEnabledHostnameSubKey() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE)
                        .setApplicationName(TEST_APPLICATION_NAME).enableHostnameSubKey().start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, TRUE);

        // no easy way to sync on change - just wait for a bit
        Thread.sleep(200);
        assertTrue(cfs.isEnabled(FEATURE_SWITCH2));
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + hostname, TRUE);
        curatorFrameworkClient.setData().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME, FALSE);

        // no easy way to sync on change - just wait for a bit
        Thread.sleep(200);
        assertFalse(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test
    public void testHostNameAndApplicationNameOverride() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE)
                        .setApplicationName(TEST_APPLICATION_NAME).enableHostnameSubKey().start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, TRUE);
        assertTrue(cfs.isEnabled(FEATURE_SWITCH2));
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + hostname, TRUE);
        curatorFrameworkClient.setData().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME, TRUE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME + "/" + hostname, FALSE);

        // no easy way to sync on change - just wait for a bit
        Thread.sleep(200);
        assertFalse(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test
    public void testHostNameAndApplicationNameOverrideInvertedValues() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE)
                        .setApplicationName(TEST_APPLICATION_NAME).enableHostnameSubKey().start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, FALSE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + hostname, FALSE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME, FALSE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME + "/" + hostname, TRUE);
        assertTrue(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test
    public void testHostNameAndApplicationNameOverrideMixedValues() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE)
                        .setApplicationName(TEST_APPLICATION_NAME).enableHostnameSubKey().start();
        CuratorFramework curatorFrameworkClient = cfs.getCuratorFrameworkClient();
        curatorFrameworkClient.create().forPath(TEST_FEATURE_SWITCH_NAMESPACE_NODE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2, FALSE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + hostname, FALSE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME, TRUE);
        curatorFrameworkClient.create().forPath(ZK_PATH_FS2 + "/" + TEST_APPLICATION_NAME + "/" + hostname, FALSE);
        assertFalse(cfs.isEnabled(FEATURE_SWITCH2));
        cfs.stop();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoConfigurationChangeAllowedForRunningSystem() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE).start();
        cfs.disableHostnameSubKey();
        fail("Should not be able to change configuration while service is running");
    }

    @Test(expected = IllegalStateException.class)
    public void testServiceMustBeRunningForIsEnabled() throws Exception {
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE).start();
        cfs.stop();
        cfs.isEnabled(FEATURE_SWITCH2);
        fail("Should not be able to call isEnabled when service has not been started");
    }

    @Test(expected = IllegalStateException.class)
    public void testServiceMustBeStartedForIsEnabled() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient("localhost:2181", new ExponentialBackoffRetry(100, 1));
        ZKFeatureSwitchService cfs =
                new ZKFeatureSwitchService().setFeatureSwitchNamespace(TEST_FEATURE_SWITCH_NAMESPACE).setCuratorFrameworkClient(
                        client);
        cfs.isEnabled(FEATURE_SWITCH2);
        fail("Should not be able to call isEnabled when service has not been started");
    }
}
